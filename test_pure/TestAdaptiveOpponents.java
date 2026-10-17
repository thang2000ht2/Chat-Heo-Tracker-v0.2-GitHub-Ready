package vn.chatheotracker;
import java.util.*;

public final class TestAdaptiveOpponents {
    static void ok(boolean yes, String message) { if (!yes) throw new AssertionError(message); }
    static List<String> cards(String x) { return Arrays.asList(x.split(" +")); }
    static String key(boolean right) {
        char[] x = new char[SeatIdentityTracker.BITS];
        Arrays.fill(x, '0');
        for(int i=12;i<320;i++) if ((i % 4 == 0) == right) x[i]='1';
        return new String(x);
    }
    public static void main(String[] args){
        LearnedOpponentHistory history=new LearnedOpponentHistory();
        history.record(cards("JS JC"));history.record(cards("9S"));history.record(cards("9H"));
        String encoded=history.encode();
        LearnedOpponentHistory reloaded=LearnedOpponentHistory.decode(encoded);
        ok(reloaded.size()==3,"learned moves survive a serialize/reload");
        ok(reloaded.snapshot().get(0).equals(cards("JS JC")),"chronology preserved");
        OpponentBehaviorModel.Profile profile=new OpponentBehaviorModel.Profile(reloaded.snapshot());
        ok(profile.brokenPairs==1 && profile.pairs==1,"real played pairs and split pair update model");
        LearnedOpponentHistory across=new LearnedOpponentHistory();
        across.record(cards("9S"));across.newRound();across.record(cards("9H"));
        OpponentBehaviorModel.Profile separate=new OpponentBehaviorModel.Profile(
            LearnedOpponentHistory.decode(across.encode()).snapshot());
        ok(separate.brokenPairs==0,"singles from different hands must NOT prove a split pair");
        for(int i=0;i<150;i++)history.record(cards("3S"));
        ok(history.size()==LearnedOpponentHistory.LIMIT,"history bounded");
        ok(LearnedOpponentHistory.decode("v1|bad@;3S").size()==1,"invalid records skipped");
        String first=key(true), second=key(false);
        ok(SeatIdentityTracker.valid(first) && SeatIdentityTracker.valid(second),"fingerprints valid");
        SeatIdentityTracker tracker=new SeatIdentityTracker(null);
        for(int i=0;i<8;i++)ok(tracker.observe(first,true)==SeatIdentityTracker.Event.NONE,"startup debounced");
        ok(tracker.observe(first,true)==SeatIdentityTracker.Event.FIRST_SEEN,"first occupant confirmed");
        String flicker=first.substring(0,300)+(first.charAt(300)=='0'?'1':'0')+first.substring(301);
        for(int i=0;i<20;i++)ok(tracker.observe(flicker,true)==SeatIdentityTracker.Event.NONE,"minor OCR flicker never replaces");
        for(int i=0;i<8;i++)ok(tracker.observe(second,true)==SeatIdentityTracker.Event.NONE,"new person debounced");
        ok(tracker.observe(second,true)==SeatIdentityTracker.Event.REPLACED,"new person resets slot");
        for(int i=0;i<25;i++)ok(tracker.observe(null,false)==SeatIdentityTracker.Event.NONE,"full-screen menu does not vacate seat");
        for(int i=0;i<13;i++)ok(tracker.observe(null,true)==SeatIdentityTracker.Event.NONE,"temporary disappearance filtered");
        ok(tracker.observe(null,true)==SeatIdentityTracker.Event.VACATED,"stable disappearance vacates seat");
        ArrayList<String> deck=new ArrayList<>();
        for(String r:MonteCarloAdvisor.RANKS)for(String suit:MonteCarloAdvisor.SUITS)deck.add(r+suit);
        List<List<List<String>>> learned=new ArrayList<>();
        learned.add(reloaded.snapshot());learned.add(new ArrayList<>());learned.add(new ArrayList<>());
        MonteCarloAdvisor.Advice suggestion=MonteCarloAdvisor.adviseBehaviorAware(
            deck.subList(0,13),Collections.emptyList(),Collections.emptyList(),new int[]{13,13,13},learned,StrategyMode.PLACEMENT);
        ok(suggestion.title.startsWith("ĐÁNH "),"strategy produces a valid suggestion");
        ok(suggestion.detail.contains("mô phỏng") || suggestion.detail.contains("chưa hiệu chuẩn"),
            "uncalibrated risk is explicit and not a fake per-seat percentage");
        System.out.println("TestAdaptiveOpponents OK (session changes, local learning, cautious advice)");
    }
}
