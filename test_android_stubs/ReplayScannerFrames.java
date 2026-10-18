package vn.chatheotracker;

import android.content.Context;
import android.graphics.Bitmap;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** RESEARCH ONLY: deterministic JVM replay, no device or API connections.
 * Input CSV: elapsed_ms,filename (relative to manifest directory).
 * Replays the same frame recognizers + counter policy + MoveEvidenceFusion used
 * by the app, and writes frame-level evidence to stdout. It is not a full
 * Android MediaProjection/UX replay or a replacement for labeled video tests.
 */
public final class ReplayScannerFrames {
    private static String csv(String s){return "\""+s.replace("\"","\"\"")+"\"";}
    public static void main(String[] args)throws Exception{
        if(args.length!=1){System.err.println("Usage: ReplayScannerFrames frames.csv");System.exit(2);}
        Path manifest=Paths.get(args[0]);
        List<String> lines=Files.readAllLines(manifest,StandardCharsets.UTF_8);
        Context context=new Context();
        CountRecognizer counters=new CountRecognizer(context);
        HandRecognizer hands=new HandRecognizer(context);
        CardRecognizer boards=new CardRecognizer(context);
        MoveEvidenceFusion fusion=new MoveEvidenceFusion();
        int[] verified={-1,-1,-1};
        int[] candidate={-1,-1,-1},streak={0,0,0};
        ArrayList<MoveEvidenceFusion.CounterChange> drops=new ArrayList<>();
        int frame=0;
        System.out.println("frame,time_ms,hand,board,raw_counters,verified_counters,matched");
        for(String line:lines){
            if(line.trim().isEmpty()||line.trim().startsWith("#")||line.startsWith("elapsed_ms"))continue;
            String[] fields=line.split(",",2);
            if(fields.length!=2)throw new IllegalArgumentException("bad manifest line: "+line);
            long now=Long.parseLong(fields[0].trim());
            Path path=manifest.toAbsolutePath().getParent().resolve(fields[1].trim());
            BufferedImage img=ImageIO.read(path.toFile());
            if(img==null)throw new IllegalArgumentException("invalid frame "+path);
            Bitmap image=new Bitmap(img);
            CountRecognizer.Details read=counters.readDetailed(image);
            HandRecognizer.Result hand=hands.recognizeVisibleHand(image);
            List<String> board=boards.recognizeCurrentPlay(image);
            fusion.observe(board,now);
            for(int seat=0;seat<3;seat++){
                int count=read.counts[seat];
                if(CounterTransitionPolicy.rejection(verified[seat],count)!=null){
                    candidate[seat]=-1;streak[seat]=0;continue;
                }
                if(candidate[seat]==count)streak[seat]++;else {candidate[seat]=count;streak[seat]=1;}
                if(streak[seat]<CounterStabilityGate.requiredFrames(verified[seat],count))continue;
                int previous=verified[seat];verified[seat]=count;
                if(CounterTransitionPolicy.mayRecordAsMove(previous,count))
                    drops.add(new MoveEvidenceFusion.CounterChange(seat,previous-count,now));
            }
            MoveEvidenceFusion.Outcome matched=fusion.match(drops,Collections.emptySet(),
                hand.complete?hand.cards:Collections.emptyList(),board,now);
            String match="";
            if(matched!=null){
                match=matched.seat+":"+matched.shape+":"+matched.cards;
                drops.remove(matched.dropIndex);
            }
            for(int i=drops.size()-1;i>=0;i--)if(now-drops.get(i).when>8000)drops.remove(i);
            System.out.println(frame+++","+now+","+csv(hand.cards.toString())+","+csv(board.toString())+
                ","+csv(Arrays.toString(read.counts))+","+csv(Arrays.toString(verified))+","+csv(match));
        }
    }
}
