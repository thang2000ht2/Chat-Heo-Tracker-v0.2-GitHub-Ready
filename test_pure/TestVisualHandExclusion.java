package vn.chatheotracker;
import java.util.*;
public class TestVisualHandExclusion {
 static void ok(boolean v,String m){if(!v)throw new AssertionError(m);}
 public static void main(String[] args){
  List<String> all=new ArrayList<>();
  for(String r:CardWarnings.RANKS)for(String suit:new String[]{"S","C","D","H"})all.add(r+suit);
  List<String> eight=Arrays.asList("4H","7C","8D","9H","10C","QC","KC","AD");
  // A visibly-held card must be gray even when history has not been reconciled.
  OpponentCandidatePool.Result unverified=OpponentCandidatePool.analyze(all,eight,false);
  ok(unverified.cue("9H")==OpponentCandidatePool.Cue.MINE,"own 9H should dim even in an inconsistent round");
  OpponentCandidatePool.Result verified=OpponentCandidatePool.analyze(all,eight,true,new int[]{9,6,6});
  ok(verified.reliable,"stable eight-card hand should yield structural possibilities");
  ok(!verified.quartets.contains("9")&&!verified.quartets.contains("8"),"owned 9H/8D break quartet");
  ok(verified.quartets.contains("3"),"four possible threes must remain detectable");
  ok(!verified.threePairs.isEmpty() && verified.threePairs.contains("7–9"),"7-8-9 pair structure should remain possible");
  System.out.println("TestVisualHandExclusion OK");
 }
}
