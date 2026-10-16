package vn.chatheotracker;

import java.util.*;

/**
 * Pure-Java, offline, capacity-constrained sampling of hidden cards.
 * Every generated world contains each unseen card exactly once and respects
 * the three visible hand counters. Visible play styles slightly tilt the
 * sampled distribution; they are NOT proof of a hidden combination.
 *
 * There is deliberately no online learning, model download or API call.
 */
final class ContextualHandSampler {
    static List<List<String>> draw(Collection<String> unseen, int[] remaining,
                                   OpponentBehaviorModel.Profile[] profiles, Random rng) {
        if (remaining == null || remaining.length != 3 || rng == null || unseen == null)
            throw new IllegalArgumentException("Invalid partition");
        int count=0;
        for(int n:remaining){ if(n<0 || n>13)throw new IllegalArgumentException("Invalid opponent count"); count+=n; }
        if(count!=unseen.size() || new HashSet<>(unseen).size()!=unseen.size())
            throw new IllegalArgumentException("Partition does not conserve 52 cards");
        ArrayList<String> shuffled=new ArrayList<>(unseen);
        Collections.shuffle(shuffled,rng);
        ArrayList<List<String>> result=new ArrayList<>(3);
        int[] free=remaining.clone();
        int[][] ranks=new int[3][13];
        for(int p=0;p<3;p++)result.add(new ArrayList<>());
        for(String card:shuffled){
            int rank=MonteCarloAdvisor.rank(card);
            if(rank<0)throw new IllegalArgumentException("Unknown card " + card);
            double[] weights=new double[3];double sum=0;
            for(int p=0;p<3;p++){
                if(free[p]<=0)continue;
                OpponentBehaviorModel.Profile profile=(profiles!=null && p<profiles.length)?profiles[p]:null;
                double weight=free[p] * contextMultiplier(ranks[p],rank,profile);
                weights[p]=weight;sum+=weight;
            }
            if(sum<=0)throw new IllegalStateException("No free card slots");
            double roll=rng.nextDouble()*sum;
            int selected=-1;
            for(int p=0;p<3;p++)if(free[p]>0){
                selected=p;
                roll-=weights[p];
                if(roll<=0)break;
            }
            result.get(selected).add(card);
            free[selected]--;ranks[selected][rank]++;
        }
        for(int p=0;p<3;p++)if(free[p]!=0)throw new IllegalStateException("Partition mismatch");
        return result;
    }
    static double contextMultiplier(int[] already, int rank, OpponentBehaviorModel.Profile profile){
        if(profile==null || profile.moves<2)return 1.0;
        // Require evidence before biasing hidden cards; cap it to prevent
        // over-fitting when only 2-3 plays have been observed.
        double reliability=Math.min(.65,profile.moves/12.0);
        double grouped=(profile.pairs+profile.triples+profile.fours+profile.straights*.45)
                /(double)Math.max(1,profile.moves);
        double fragments=profile.singles/(double)Math.max(1,profile.moves);
        double groupBias=Math.max(-.65,Math.min(.65,2*(grouped-fragments)));
        int same=already[rank];
        double factor=1.0;
        if(same>0){
            factor+=reliability*groupBias*(same==1?.65:.48);
        }
        int neighbor=(rank>0?already[rank-1]:0)+(rank<11?already[rank+1]:0);
        if(rank<12 && neighbor>0 && profile.straights>0){
            factor+=reliability*.12*Math.min(2,neighbor);
        }
        return Math.max(.70,Math.min(1.35,factor));
    }
    private ContextualHandSampler(){}
}
