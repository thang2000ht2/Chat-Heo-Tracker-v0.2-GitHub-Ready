package vn.chatheotracker;
import android.content.Context;
import android.graphics.Bitmap;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.io.*;
import java.util.*;

/** Actual CountRecognizer on manually labeled real-game badge crops.
 * Never treats multiple crops of the same frame as independent statistics. */
public final class TestCountRealImages {
    public static void main(String[] args)throws Exception {
        CountRecognizer recognizer=new CountRecognizer(new Context());
        int[][] centers={{1174,148},{335,386},{1744,386}};
        int correct=0,wrong=0,abstain=0,total=0;
        int[] seatTotal={0,0,0},seatCorrect={0,0,0},seatWrong={0,0,0},seatAbstain={0,0,0};
        try(BufferedReader br=new BufferedReader(new FileReader("test_images/counters/labels.csv"))){
            String line=br.readLine();
            while((line=br.readLine())!=null){
                String[] fields=line.split(",");
                String id=fields[0];int seat=Integer.parseInt(fields[1]),truth=Integer.parseInt(fields[2]);
                BufferedImage crop=ImageIO.read(new File("test_images/counters/"+id+".png"));
                BufferedImage full=new BufferedImage(2048,942,BufferedImage.TYPE_INT_RGB);
                Graphics2D g=full.createGraphics();g.drawImage(crop,centers[seat][0]-62,centers[seat][1]-60,null);g.dispose();
                CountRecognizer.Details got=recognizer.readDetailed(new Bitmap(full));
                int v=got.counts[seat];total++;seatTotal[seat]++;
                if(v==truth){correct++;seatCorrect[seat]++;}
                else if(v<0){abstain++;seatAbstain[seat]++;}
                else {wrong++;seatWrong[seat]++;}
                System.out.printf("%s expected=%d read=%d best=%.3f second=%.3f margin=%.3f %s%n",
                    id,truth,v,got.best[seat],got.runner[seat],got.margin[seat],got.reasons[seat]);
            }
        }
        System.out.printf("REAL COUNTER CROPS: total=%d correct=%d wrong=%d abstain=%d%n",total,correct,wrong,abstain);
        for(int seat=0;seat<3;seat++)
            System.out.printf("seat %d: labeled=%d correct=%d wrong=%d abstain=%d%n",
                seat,seatTotal[seat],seatCorrect[seat],seatWrong[seat],seatAbstain[seat]);
        if(wrong!=0)throw new AssertionError("Any wrong digit on labeled crops blocks this build");
        if(correct<total*0.75)throw new AssertionError("Excessive abstention on available real crops");
        BufferedImage black=new BufferedImage(2048,942,BufferedImage.TYPE_INT_RGB);
        int[] absent=recognizer.readDetailed(new Bitmap(black)).counts;
        if(absent[0]!=-1||absent[1]!=-1||absent[2]!=-1)
            throw new AssertionError("Missing orange badge must remain UNKNOWN, not 0");
    }
}
