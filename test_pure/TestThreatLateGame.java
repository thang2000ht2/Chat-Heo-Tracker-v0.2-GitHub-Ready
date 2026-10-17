package vn.chatheotracker;
import java.util.*;
public class TestThreatLateGame {
    static void ok(boolean condition){if(!condition)throw new AssertionError();}
    public static void main(String[] args){
        List<String> mine=new ArrayList<>();
        for(String r:MonteCarloAdvisor.RANKS)mine.add(r+"S");
        List<List<List<String>>> history=Arrays.asList(Collections.emptyList(),Collections.emptyList(),Collections.emptyList());
        // Example with 7 observed plays by the left player, and no invented cards.
        List<String> observed=Arrays.asList("3C","4C","5C","6C","7C","8C","9C");
        ThreatScenarioSampler.Seat[] s=ThreatScenarioSampler.sample(mine,observed,new int[]{6,13,13},history,13);
        ok(s[0].samples==ThreatScenarioSampler.SAMPLES);
        ok(s[0].percent(1)==0); // four pairs require at least eight cards
        // After two further cards, all three-pair runs still require six cards.
        List<String> more=new ArrayList<>(observed);more.add("10C");more.add("JC");
        s=ThreatScenarioSampler.sample(mine,more,new int[]{4,13,13},history,13);
        ok(s[0].samples==ThreatScenarioSampler.LATE_SAMPLES);
        ok(s[0].percent(0)==0&&s[0].percent(1)==0);
        System.out.println("TestThreatLateGame OK");
    }
}
