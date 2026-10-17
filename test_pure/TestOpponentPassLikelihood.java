package vn.chatheotracker;
import java.util.*;
public final class TestOpponentPassLikelihood {
    static void require(boolean ok,String info){if(!ok)throw new AssertionError(info);}
    public static void main(String[] args){
        ObservedPass event=new ObservedPass(Arrays.asList("10S"),7);
        double canBeat=event.likelihood(Arrays.asList("JS","3C","8H"),7);
        double cannotBeat=event.likelihood(Arrays.asList("3S","4C","8H"),7);
        require(canBeat<cannotBeat,"Confirmed pass must weakly discount worlds with a legal reply");
        require(canBeat>0,"A pass never proves absence of a reply");
        require(event.likelihood(Arrays.asList("JS"),4)>canBeat,"A stale pass weakens after cards are shed");
        require(!ObservedPass.canBeat(Arrays.asList("7S","9S"),Arrays.asList("10S")),"Cannot beat single 10");
        require(ObservedPass.canBeat(Arrays.asList("JS","9S"),Arrays.asList("10S")),"Can beat single 10");
        require(ObservedPass.canBeat(Arrays.asList("KS","KC"),Arrays.asList("JS","JC")),"Pair K can beat pair J");
        require(!ObservedPass.canBeat(Arrays.asList("KS"),Arrays.asList("JS","JC")),"One K cannot beat pair J");
        int[] rank=new int[13];rank[0]=2;rank[1]=2;rank[2]=2;
        boolean[] combos=ThreatScenarioSampler.features(rank);
        require(combos[0]&&!combos[1]&&!combos[2],"Three adjacent pairs detected");
        rank[3]=2;combos=ThreatScenarioSampler.features(rank);
        require(combos[0]&&combos[1],"Four adjacent pairs detected");
        rank[10]=4;combos=ThreatScenarioSampler.features(rank);
        require(combos[2],"Quartet detected");
        // Partial hand and missing play history may still provide a *weak* sample.
        List<List<List<String>>> empty=Arrays.asList(Collections.emptyList(),Collections.emptyList(),Collections.emptyList());
        ThreatScenarioSampler.Seat[] weak=ThreatScenarioSampler.sample(
            Arrays.asList("3S","4S"),Arrays.asList("5S"),new int[]{10,8,6},empty,1234);
        require(weak.length==3&&weak[0].lowConfidence,"Incomplete state is labelled low confidence");
        for(ThreatScenarioSampler.Seat seat:weak)for(int i=0;i<3;i++)
            require(seat.percent(i)>=0&&seat.percent(i)<=100,"Percent bounded");
        System.out.println("TestOpponentPassLikelihood OK");
    }
}
