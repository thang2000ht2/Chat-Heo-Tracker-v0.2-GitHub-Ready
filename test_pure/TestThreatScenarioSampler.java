package vn.chatheotracker;
import java.util.*;
public final class TestThreatScenarioSampler {
    static void require(boolean value){if(!value)throw new AssertionError();}
    public static void main(String[] args){
        List<String> hand=new ArrayList<>(),played=new ArrayList<>();
        String[] ranks=MonteCarloAdvisor.RANKS,suits=MonteCarloAdvisor.SUITS;
        for(int i=0;i<13;i++)hand.add(ranks[i]+suits[0]);
        int[] counters={13,13,13};
        List<List<List<String>>> profiles=Arrays.asList(Collections.emptyList(),Collections.emptyList(),Collections.emptyList());
        ThreatScenarioSampler.Seat[] result=ThreatScenarioSampler.sample(hand,played,counters,profiles,42);
        require(result.length==3);
        for(ThreatScenarioSampler.Seat p:result){
            require(p.samples==ThreatScenarioSampler.SAMPLES);
            for(int i=0;i<3;i++)require(p.hits[i]>=0 && p.hits[i]<=p.samples);
        }
        ThreatScenarioSampler.Seat[] weak=ThreatScenarioSampler.sample(hand,played,new int[]{5,5,5},profiles,42);
        require(weak.length==3 && weak[0].lowConfidence);
        require(weak[0].samples==ThreatScenarioSampler.LATE_SAMPLES);
        try {ThreatScenarioSampler.sample(hand,played,new int[]{-1,13,13},profiles,42);throw new AssertionError("Need guard on unknown counters");}
        catch(IllegalArgumentException expected){}
        System.out.println("ThreatScenarioSampler OK");
    }
}
