package vn.chatheotracker;
import java.util.*;
public final class TestPartialHistoryRecovery {
    private static void check(boolean c,String m){if(!c)throw new AssertionError(m);}
    static ArrayList<String> deck(){
        ArrayList<String> d=new ArrayList<>();
        for(String r:MonteCarloAdvisor.RANKS)for(String s:MonteCarloAdvisor.SUITS)d.add(r+s);
        return d;
    }
    public static void main(String[] args){
        ArrayList<String> d=deck();
        List<String> mine=d.subList(0,13);
        List<String> played=d.subList(13,18);
        PartialDeckState.Result partial=PartialDeckState.check(mine,played,new int[]{10,10,10});
        check(partial.feasible&&!partial.exact&&partial.missing==4,"Missing history recognized");
        MonteCarloAdvisor.Advice tip=MonteCarloAdvisor.adviseBehaviorAware(
            mine,Collections.emptyList(),played,new int[]{10,10,10},null,StrategyMode.PLACEMENT);
        check(tip.title.startsWith("ĐÁNH"),"Advice survives 4 missed played cards: "+tip.title);
        check(tip.detail.contains("thiếu 4 lá lịch sử"),"Estimated risk labeled partial");
        check(!PartialDeckState.check(mine,played,new int[]{13,13,13}).feasible,"Impossible snapshot refused");
        ArrayList<String> unknown=new ArrayList<>(d.subList(13,52));
        Random rng=new Random(141);
        for(int trial=0;trial<100;trial++){
            List<List<String>> hands=ContextualHandSampler.drawWithUnobserved(unknown,
                new int[]{10,10,10},null,rng);
            HashSet<String> each=new HashSet<>();
            for(int p=0;p<3;p++){
                check(hands.get(p).size()==10,"Count mismatch");
                for(String card:hands.get(p))check(each.add(card),"Duplicate hidden card");
            }
            check(each.size()==30,"Exactly three visible opponent hands");
        }
        TemporalHandEvidence evidence=new TemporalHandEvidence();
        List<String> all=new ArrayList<>(mine);
        for(int i=0;i<6;i++){
            ArrayList<String> slots=new ArrayList<>(all);
            slots.set(i%3,null);
            List<String> got=evidence.accept(slots,13,410);
            if(i<3)check(got.isEmpty(),"No early hand guesses");
        }
        check(evidence.accept(all,13,410).size()==13,"Occluded positions recovered across frames");
        check(evidence.accept(all,13,440).isEmpty(),"Moving fan resets votes");
        check(HeoWarningAdvisor.warningForMove(Arrays.asList("3S","4S"),Collections.emptyList(),0.8).isEmpty(),
            "Do not warn without playing a heo");
        check(HeoWarningAdvisor.warningForMove(Arrays.asList("2H"),Arrays.asList("JS","JC"),0.8).isEmpty(),
            "Single heo cannot answer an opponent pair");
        check(HeoWarningAdvisor.warningForMove(Arrays.asList("2H"),Arrays.asList("AS"),0.25).contains("chưa định lượng"),
            "Give estimated chop risk only for proposed heo play");
        System.out.println("TestPartialHistoryRecovery OK");
    }
}
