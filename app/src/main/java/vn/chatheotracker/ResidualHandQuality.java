package vn.chatheotracker;

import java.util.Collection;

/** Small, deterministic tie-breaker for the shape of the cards left in our hand.
 * Lower is better. It is not a probability, and never overrides legality.
 */
final class ResidualHandQuality {
    static double penalty(Collection<String> remaining){
        if(remaining==null || remaining.isEmpty())return 0;
        int[] groups=new int[13];
        for(String card:remaining){
            if(card==null || card.length()<2)continue;
            String rank=card.substring(0,card.length()-1);
            for(int i=0;i<MonteCarloAdvisor.RANKS.length;i++)
                if(MonteCarloAdvisor.RANKS[i].equals(rank)){groups[i]++;break;}
        }
        int stranded=0,connected=0,control=0;
        for(int i=0;i<13;i++){
            if(groups[i]==1){
                boolean near=(i>0 && groups[i-1]>0 && i<12)
                    || (i<11 && groups[i+1]>0);
                if(!near && i<10)stranded++;
            }
            if(groups[i]>=2)connected+=groups[i]-1;
            if(i>=10 && groups[i]>0)control+=groups[i];
        }
        double score=0.14*stranded-0.07*connected;
        // If we have to play 4+ cards later without A/K/2, control is harder.
        if(remaining.size()>=4 && control==0)score+=0.16;
        return Math.max(-0.28,Math.min(1.3,score));
    }
    private ResidualHandQuality(){}
}
