package vn.chatheotracker;
import java.util.*;
public final class TestOpeningTempoPolicy {
    static void ok(boolean b,String m){if(!b)throw new AssertionError(m);}
    static List<String> hand(String t){return Arrays.asList(t.split(" +"));}
    public static void main(String[] args){
        List<String> mine=hand("2S 2C 2H 4S 6D 7H 8C 9D JD QH KC AS 3H");
        double heos=OpeningTempoPolicy.extraCost(mine,hand("2S 2C 2H"),13,StrategyMode.PLACEMENT);
        double four=OpeningTempoPolicy.extraCost(mine,hand("4S"),13,StrategyMode.PLACEMENT);
        ok(heos>four+15,"Start of hand should strongly preserve twos vs isolated 4");
        ok(OpeningTempoPolicy.extraCost(mine,hand("2S 2C 2H"),2,StrategyMode.PLACEMENT)==0,
            "Do not save twos mechanically with rival about to finish");
        ok(OpeningTempoPolicy.extraCost(hand("4S 2H"),hand("4S 2H"),13,StrategyMode.PLACEMENT)==0,
            "Immediate finish must never be delayed by opening priors");
        // An independent hand-level behavioral Monte Carlo test, with no public moves.
        MonteCarloAdvisor.Advice a=MonteCarloAdvisor.adviseWithIncompleteHistory(
            mine,Collections.emptyList(),Collections.emptyList(),StrategyMode.PLACEMENT);
        ok(a.title.startsWith("ĐÁNH"),"Produce a legal recommendation");
        ok(!a.title.contains("2♠ 2♣ 2♥"),"Do not unload all three twos in the opening: "+a.title);
        MonteCarloAdvisor.Advice full=MonteCarloAdvisor.adviseBehaviorAware(
            mine,Collections.emptyList(),Collections.emptyList(),new int[]{13,13,13},
            null,StrategyMode.PLACEMENT);
        ok(full.title.startsWith("ĐÁNH"),"Complete deal should produce a recommendation: "+full.title);
        ok(!full.title.contains("2♠ 2♣ 2♥"),"Do not recommend three twos right after dealing: "+full.title);
        System.out.println("TestOpeningTempoPolicy OK -> fallback "+a.title+", complete "+full.title);
    }
}
