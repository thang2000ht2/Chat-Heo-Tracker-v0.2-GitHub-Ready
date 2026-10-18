package vn.chatheotracker;
import java.util.*;
public class TestLearnedCorrection {
 public static void main(String[] a){
    LearnedOpponentHistory h=new LearnedOpponentHistory();
    h.record(Arrays.asList("JS","QH"));h.newRound();
    h.record(Arrays.asList("JD","JH","KS"));
    h.record(Arrays.asList("7C"));
    int n=h.retractCurrentRound(Arrays.asList("JH","KS"));
    if(n!=1)throw new AssertionError("corrected one suspect move");
    java.util.List<java.util.List<String>> snap=h.snapshot();
    if(!snap.get(0).equals(Arrays.asList("JS","QH")))throw new AssertionError("must not erase historical deals");
    if(!snap.get(2).equals(Collections.singletonList("JD")))throw new AssertionError("retain only verified card of the move");
    if(h.retractCurrentRound(Collections.singletonList("JS"))!=0)throw new AssertionError("old round preserved");
    System.out.println("TestLearnedCorrection OK");
 }
}
