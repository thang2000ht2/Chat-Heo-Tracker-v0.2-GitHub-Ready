package vn.chatheotracker;
import java.util.*;
/** Synthetic animation replay across 3..12 cards; not a real-video accuracy benchmark. */
public final class TestMultiLengthEvidenceReplay {
    private static void ok(boolean v,String m){if(!v)throw new AssertionError(m);}
    public static void main(String[] args){
        String[] rank={"3","4","5","6","7","8","9","10","J","Q","K","A"};
        String[] suit={"S","H","D","C"};
        for(int n=3;n<=12;n++){
            ArrayList<String> move=new ArrayList<>();
            for(int i=0;i<n;i++)move.add(rank[i]+suit[i%4]);
            ok(PlayStructure.classify(move).accepted(),"straight length="+n);
            MoveEvidenceFusion matcher=new MoveEvidenceFusion();
            long first=4000;
            // Partial, progressive visibility: animated cards become readable gradually.
            matcher.observe(move.subList(0,Math.min(n,2)),first);
            matcher.observe(move.subList(0,Math.min(n,5)),first+200);
            matcher.observe(move,first+400);
            MoveEvidenceFusion.Outcome result=matcher.match(
                Collections.singletonList(new MoveEvidenceFusion.CounterChange(1,n,first+350)),
                Collections.emptySet(),Collections.emptySet(),move,first+500);
            ok(result!=null && result.cards.size()==n && result.seat==1,
                "length "+n+" matched as ONE verified play");
            matcher.clear();
            matcher.observe(move.subList(0,n-1),first);
            ok(matcher.match(
                Collections.singletonList(new MoveEvidenceFusion.CounterChange(1,n,first+100)),
                Collections.emptySet(),Collections.emptySet(),move.subList(0,n-1),first+120)==null,
                "length "+n+" missing a card must not be invented");
        }
        System.out.println("TestMultiLengthEvidenceReplay OK");
    }
}
