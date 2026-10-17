package vn.chatheotracker;
import java.util.*;
public final class TestOpponentBehavior {
    static void ok(boolean condition,String description){if(!condition)throw new AssertionError(description);}
    static List<String> hand(String s){return Arrays.asList(s.split(" +"));}
    static List<List<List<String>>> emptyHistory(){
        ArrayList<List<List<String>>> all=new ArrayList<>();
        for(int i=0;i<3;i++)all.add(new ArrayList<>());
        return all;
    }
    public static void main(String[] args){
        OpponentBehaviorModel.Profile neutral=new OpponentBehaviorModel.Profile(Collections.emptyList());
        List<String> paired=hand("JS JC 3S 4S 7C 8D");
        List<String> unpaired=hand("JS 3S 4S 7C 8D 9H");
        double holdsPair=OpponentBehaviorModel.singleReplyChance(paired,"10H",6,neutral);
        double canSpendSingle=OpponentBehaviorModel.singleReplyChance(unpaired,"10H",6,neutral);
        ok(holdsPair<canSpendSingle,"J pair should be less likely to split to overtake a single 10");
        ok(holdsPair>0,"splitting a pair is possible, never treated as impossible");
        double holdsTriple=OpponentBehaviorModel.singleReplyChance(
            hand("JS JC JH 3S 4S 7C"),"10H",6,neutral);
        double holdsFour=OpponentBehaviorModel.singleReplyChance(
            hand("JS JC JD JH 3S 7C"),"10H",6,neutral);
        ok(holdsTriple<holdsPair,"triple should be harder to split than pair");
        ok(holdsFour<holdsTriple,"four of a kind should be harder to split than triple");
        List<List<String>> broke=new ArrayList<>();
        broke.add(hand("9S"));broke.add(hand("9H"));
        OpponentBehaviorModel.Profile pBroke=new OpponentBehaviorModel.Profile(broke);
        ok(pBroke.brokenPairs==1,"two separate singleton 9s prove an observed split pair");
        ok(OpponentBehaviorModel.singleReplyChance(paired,"10H",6,pBroke)>holdsPair,
            "observed pair splitting increases willingness to split J pair");
        double urgency=OpponentBehaviorModel.singleReplyChance(paired,"10H",2,neutral);
        ok(urgency>holdsPair,"near-empty hands are more willing to break groups");
        List<String> mine=hand("3S 3C 4H 5S 5C 6S 7H 8H 9S 10C JC QC 2H");
        MonteCarloAdvisor.Advice advice=MonteCarloAdvisor.adviseBehaviorAware(mine,
            Collections.emptyList(),Collections.emptyList(),new int[]{13,13,13},emptyHistory(),StrategyMode.PLACEMENT);
        ok(advice.title.startsWith("ĐÁNH "),"full consistent deal gets a concrete play "+advice.title);
        ok(advice.detail.contains("mức chặn ước tính"),"risk must be labelled heuristic");
        MonteCarloAdvisor.Advice invalid=MonteCarloAdvisor.adviseBehaviorAware(mine,
            Collections.emptyList(),Collections.emptyList(),new int[]{12,13,13},emptyHistory(),StrategyMode.PLACEMENT);
        ok(invalid.title.startsWith("ĐÁNH") && invalid.detail.contains("thiếu 1 lá lịch sử"),
            "missing public history is handled without inventing the card");
        MonteCarloAdvisor.Advice impossible=MonteCarloAdvisor.adviseBehaviorAware(mine,
            Collections.emptyList(),Collections.singletonList("4D"),
            new int[]{13,13,13},emptyHistory(),StrategyMode.PLACEMENT);
        ok(impossible.title.startsWith("QUÉT LẠI"),"an impossible total still blocks suggestions");
        MonteCarloAdvisor.Advice noHeo=MonteCarloAdvisor.adviseBehaviorAware(
            hand("3S 3C 4H 5S 5C 6S 7H 8H 9S 10C JC QC KH"),
            Collections.emptyList(),Collections.emptyList(),new int[]{13,13,13},emptyHistory(),StrategyMode.PLACEMENT);
        ok(!noHeo.detail.contains("⚠ HEO"),"no heo => no chop warning");
        Random rng=new Random(20261017L);
        ArrayList<String> deck=new ArrayList<>();
        for(String rank:MonteCarloAdvisor.RANKS)for(String suit:MonteCarloAdvisor.SUITS)deck.add(rank+suit);
        for(int i=0;i<40;i++){
            Collections.shuffle(deck,rng);
            MonteCarloAdvisor.Advice trial=MonteCarloAdvisor.adviseBehaviorAware(
                deck.subList(0,13),Collections.emptyList(),Collections.emptyList(),
                new int[]{13,13,13},emptyHistory(),StrategyMode.PLACEMENT);
            ok(trial.title.startsWith("ĐÁNH "),"valid random hand "+i+": "+trial.title);
            ok(!trial.detail.contains("Chế độ") && trial.detail.length()<150,"concise output "+i);
        }
        System.out.println("TestOpponentBehavior OK (40 deals; pairs/triples/fours, history and urgency)");
    }
}
