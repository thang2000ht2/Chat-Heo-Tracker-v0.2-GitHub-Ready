package vn.chatheotracker;

import java.util.*;

/** Hypothetical prevalence of card combinations in sampled hidden hands.
 * Important: not a trained/calibrated probability of a real opponent possessing a combo,
 * and absolutely not the probability they will use it to chop. Do not print as a risk %. */
final class ThreatScenarioSampler {
    static final int SAMPLES=512;
    static final class Seat {
        final int[] hits; final int samples;
        Seat(int[] counts,int n){hits=counts;samples=n;}
        int percent(int i){return (int)(5*Math.round(20.0*hits[i]/Math.max(1,samples)));}
        String concise(){return "3ĐT ~"+percent(0)+"% · 4ĐT ~"+percent(1)+"% · TQ ~"+percent(2)+"% mẫu";}
    }
    static Seat[] sample(Collection<String> myHand,Collection<String> seen,int[] remaining,
                         List<List<List<String>>> histories,long seed){
        PartialDeckState.Result chk=PartialDeckState.check(myHand,seen,remaining);
        if(!chk.feasible || chk.missing>4)throw new IllegalArgumentException("Unreconciled card counts");
        Set<String> used=new HashSet<>(myHand);used.addAll(seen);
        ArrayList<String> unknown=new ArrayList<>();
        for(String r:MonteCarloAdvisor.RANKS)for(String s:MonteCarloAdvisor.SUITS){
            String card=r+s;if(!used.contains(card))unknown.add(card);
        }
        OpponentBehaviorModel.Profile[] profiles=OpponentBehaviorModel.fromHistory(histories);
        Random rnd=new Random(seed);
        int[][] hitCounts=new int[3][3];
        for(int trial=0;trial<SAMPLES;trial++){
            List<List<String>> hands=chk.exact?
                ContextualHandSampler.draw(unknown,remaining,profiles,rnd):
                ContextualHandSampler.drawWithUnobserved(unknown,remaining,profiles,rnd);
            for(int p=0;p<3;p++){
                int[] ranks=new int[13];
                for(String c:hands.get(p))ranks[MonteCarloAdvisor.rank(c)]++;
                boolean three=false,four=false,quartet=false;
                for(int r=0;r<13;r++)if(ranks[r]==4)quartet=true;
                // Rank 2 never appears in a consecutive-pair run.
                for(int start=0;start<=9;start++){
                    if(ranks[start]>=2 && ranks[start+1]>=2 && ranks[start+2]>=2)three=true;
                }
                for(int start=0;start<=8;start++){
                    if(ranks[start]>=2 && ranks[start+1]>=2 && ranks[start+2]>=2 && ranks[start+3]>=2)four=true;
                }
                if(three)hitCounts[p][0]++;
                if(four)hitCounts[p][1]++;
                if(quartet)hitCounts[p][2]++;
            }
        }
        Seat[] result=new Seat[3];
        for(int p=0;p<3;p++)result[p]=new Seat(hitCounts[p],SAMPLES);
        return result;
    }
    private ThreatScenarioSampler(){}
}
