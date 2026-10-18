package vn.chatheotracker;
import java.util.*;
public class TestObservedHandReconciler {
    static void ok(boolean v,String msg){if(!v)throw new AssertionError(msg);}
    public static void main(String[] args){
        List<String> visual=Arrays.asList("4S","7D","7H","10H","JD","QS","KS","KD","KH","AD");
        ObservedHandReconciler.Result mistaken=ObservedHandReconciler.inspect(visual,
            Arrays.asList("3S","3C","3H","3D","10H"),new int[]{-1,8,4});
        ok(mistaken.feasible,"Missing winner counter must not block a visually verified 10-card hand");
        ok(mistaken.contradictions.equals(Collections.singletonList("10H")),
            "Visible 10H must invalidate its stale public play record");
        ok(!mistaken.allCountersKnown,"A missing counter is uncertainty, not a guess");
        ObservedHandReconciler.Result clean=ObservedHandReconciler.inspect(
            Collections.singleton("9H"),Arrays.asList("4H","2D"),new int[]{11,8,8});
        ok(clean.feasible && clean.contradictions.isEmpty(),"Clean hand must be retained");
        ok(!ObservedHandReconciler.inspect(Arrays.asList("9H","9H"),
            Collections.emptyList(),new int[]{13,13,13}).feasible,"Duplicate hand must fail");
        ok(!ObservedHandReconciler.inspect(visual,Arrays.asList("3S","3C","3D","3H"),new int[]{13,13,13}).feasible,
            "Overfull deck must fail");
        // Once a 10H is visibly owned, the FOUR tens cannot form a quartet
        // in any opponent's hand even if the previous log had 10H as played.
        java.util.List<String> unplayed=new java.util.ArrayList<>();
        for(String rank:CardWarnings.RANKS)for(String suit:new String[]{"S","C","D","H"})
            unplayed.add(rank+suit);
        unplayed.removeAll(Arrays.asList("3S","3C","3H","3D"));
        OpponentCandidatePool.Result pool=OpponentCandidatePool.analyze(unplayed,visual,true,new int[]{0,8,4});
        ok(pool.reliable && pool.cue("10H")==OpponentCandidatePool.Cue.MINE,
            "10H must be dimmed as owned");
        ok(!pool.quartets.contains("10"),"Holding 10H must rule out opponents' quartet of tens");
        ok(!ObservedHandReconciler.inspect(visual,Arrays.asList("ZZ"),
            new int[]{1,1,1}).feasible,"Invalid card names must fail");
        System.out.println("TestObservedHandReconciler OK");
    }
}
