package vn.chatheotracker;

import java.util.*;

/** Hypothetical prevalence of 3/4 consecutive pairs and quartets in opponents'
 * hidden hands. This is NOT a trained probability of someone actually using a
 * combination to chop. The weighted sample conditions on current card counts,
 * confirmed public cards, mild play-style evidence and user-confirmed passes.
 */
final class ThreatScenarioSampler {
    static final int SAMPLES=1024;
    static final int LATE_SAMPLES=1536;
    static final class Seat {
        final int[] hits; final int samples;
        final double[] weights; final double totalWeight;
        final boolean lowConfidence;
        Seat(int[] h,int n,double[] w,double total,boolean low){
            hits=h;samples=n;weights=w;totalWeight=total;lowConfidence=low;
        }
        int percent(int i){
            if(i<0||i>2 || totalWeight<=0)return 0;
            return Math.max(0,Math.min(100,(int)(5*Math.round(20.0*weights[i]/totalWeight))));
        }
        String concise(){
            return "3ĐT "+percent(0)+"% · 4ĐT "+percent(1)+"% · TQ "+percent(2)+"%"+
                (lowConfidence?" ước lượng yếu":" mẫu");
        }
    }
    static Seat[] sample(Collection<String> myHand,Collection<String> seen,int[] remaining,
                         List<List<List<String>>> histories,long seed){
        return sample(myHand,seen,remaining,histories,null,seed);
    }
    static Seat[] sample(Collection<String> myHand,Collection<String> seen,int[] remaining,
                         List<List<List<String>>> histories,List<List<ObservedPass>> passes,long seed){
        if(remaining==null||remaining.length!=3)throw new IllegalArgumentException("Missing counters");
        int total=0;
        for(int n:remaining){if(n<0||n>13)throw new IllegalArgumentException("Unknown counter");total+=n;}
        Set<String> used=new HashSet<>();
        if(myHand!=null)for(String c:myHand){
            if(!RecentMoves.isCard(c) || !used.add(c))throw new IllegalArgumentException("Invalid hand");
        }
        if(seen!=null)for(String c:seen){
            if(!RecentMoves.isCard(c) || !used.add(c))throw new IllegalArgumentException("Duplicated card");
        }
        ArrayList<String> unknown=new ArrayList<>();
        for(String r:MonteCarloAdvisor.RANKS)for(String s:MonteCarloAdvisor.SUITS){
            String card=r+s;if(!used.contains(card))unknown.add(card);
        }
        if(total>unknown.size())throw new IllegalArgumentException("Impossible count total");
        boolean weak=total!=unknown.size() || (myHand==null || myHand.isEmpty());
        OpponentBehaviorModel.Profile[] profiles=OpponentBehaviorModel.fromHistory(histories);
        Random rnd=new Random(seed);
        int n=SAMPLES;
        for(int c:remaining)if(c<=5)n=LATE_SAMPLES;
        int[][] hits=new int[3][3];
        double[][] weightedHits=new double[3][3];
        double[] sumWeight=new double[3];
        for(int trial=0;trial<n;trial++){
            List<List<String>> hands=total==unknown.size()?
                ContextualHandSampler.draw(unknown,remaining,profiles,rnd):
                ContextualHandSampler.drawWithUnobserved(unknown,remaining,profiles,rnd);
            for(int p=0;p<3;p++){
                int[] ranks=new int[13];
                for(String c:hands.get(p))ranks[MonteCarloAdvisor.rank(c)]++;
                boolean[] found=features(ranks);
                double weight=passLikelihood(hands.get(p),remaining[p],passes,p);
                sumWeight[p]+=weight;
                for(int i=0;i<3;i++)if(found[i]){
                    hits[p][i]++; weightedHits[p][i]+=weight;
                }
            }
        }
        Seat[] result=new Seat[3];
        for(int p=0;p<3;p++)result[p]=new Seat(hits[p],n,weightedHits[p],sumWeight[p],weak);
        return result;
    }
    static boolean[] features(int[] ranks){
        boolean three=false,four=false,quartet=false;
        for(int r=0;r<13;r++)if(ranks[r]==4)quartet=true;
        // 2 is not in consecutive-pair runs. 3..A are positions 0..11.
        for(int s=0;s<=9;s++)if(ranks[s]>=2 && ranks[s+1]>=2 && ranks[s+2]>=2)three=true;
        for(int s=0;s<=8;s++)if(ranks[s]>=2&&ranks[s+1]>=2&&ranks[s+2]>=2&&ranks[s+3]>=2)four=true;
        return new boolean[]{three,four,quartet};
    }
    private static double passLikelihood(Collection<String> cards,int count,
            List<List<ObservedPass>> passes,int seat){
        if(passes==null || seat>=passes.size() || passes.get(seat)==null)return 1.0;
        double weight=1.0;
        // At most 5 confirmed observations from the current round. Keep a floor
        // to avoid assigning certainty to hypotheses from heuristic likelihoods.
        List<ObservedPass> observed=passes.get(seat);
        for(int i=Math.max(0,observed.size()-5);i<observed.size();i++)
            weight*=observed.get(i).likelihood(cards,count);
        return Math.max(.20,weight);
    }
    private ThreatScenarioSampler(){}
}
