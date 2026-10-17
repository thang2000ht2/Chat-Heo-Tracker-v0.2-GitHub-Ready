package vn.chatheotracker;
import java.util.*;
public final class TestBaitBehaviorSignals {
    static void ok(boolean x,String m){if(!x)throw new AssertionError(m);}
    public static void main(String[] args){
        BaitBehaviorSignals tracker=new BaitBehaviorSignals();
        BaitBehaviorSignals.Observation a=tracker.record(0,Arrays.asList("AH"),Arrays.asList("2S","3C"),12);
        ok(a.possibleBaitSignal()&&a.pressureSingle,"high solo pressure recorded");
        ok(tracker.note(0).isEmpty(),"single observation not proof");
        tracker.record(0,Arrays.asList("KH"),Arrays.asList("2S"),11);
        ok(tracker.note(0).contains("chưa rõ"),"two signals warn without declaring intent");
        ok(!BaitBehaviorSignals.fromConfirmedPlay(Arrays.asList("6H"),Arrays.asList("2S"),10).possibleBaitSignal(),"low solo no signal");
        ok(!BaitBehaviorSignals.fromConfirmedPlay(Arrays.asList("AH"),Arrays.asList("4S"),10).possibleBaitSignal(),"no known heo means no target signal");
        ok(!BaitBehaviorSignals.fromConfirmedPlay(Arrays.asList("AH"),Arrays.asList("2S"),2).possibleBaitSignal(),"finishing pressure not assumed bait");
        ok(!BaitBehaviorSignals.fromConfirmedPlay(Arrays.asList("KH","KS"),Arrays.asList("2S"),8).possibleBaitSignal(),"cannot bait a lone 2 with a high pair");
        ok(BaitBehaviorSignals.fromConfirmedPlay(Arrays.asList("KH","KS"),Arrays.asList("2S","2H"),8).possibleBaitSignal(),"pair of 2s is an eligible response");
        tracker.resetRound();ok(tracker.note(0).isEmpty(),"round separation");
        System.out.println("TestBaitBehaviorSignals OK");
    }
}
