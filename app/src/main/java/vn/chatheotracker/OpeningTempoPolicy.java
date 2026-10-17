package vn.chatheotracker;

import java.util.Collection;

/** A conservative opening tempo prior, not a claimed win-rate model.
 * Keep control cards while there is time to shed isolated low singles.
 * Decays as an opponent approaches the finish, and never delays a legal finish. */
final class OpeningTempoPolicy {
    static double extraCost(Collection<String> fullHand, Collection<String> move,
                            int minimumOpponentCards, StrategyMode mode) {
        if(fullHand==null || move==null || move.isEmpty() ||
           move.size()==fullHand.size() || fullHand.size()<7) return 0;
        double stage = minimumOpponentCards<=3 ? 0.0 :
            minimumOpponentCards<=5 ? 0.20 : minimumOpponentCards<=7 ? 0.55 : 1.0;
        if(stage==0)return 0;
        int[] fullRanks=new int[13];
        for(String c:fullHand){int r=MonteCarloAdvisor.rank(c);if(r>=0)fullRanks[r]++;}
        double cost=0;
        int twos=0;
        for(String c:move){
            int r=MonteCarloAdvisor.rank(c);
            if(r==12){twos++;cost+=mode==StrategyMode.FIRST?6.0:8.0;}
            else if(r==11)cost+=3.3; // Ace: reliable re-entry card
            else if(r==10)cost+=2.0; // King
            else if(r==9)cost+=1.1; // Queen
        }
        if(twos>=2)cost+=3.0; // Do not unload a strong pig group on deal 1
        if(move.size()==1){
            int r=MonteCarloAdvisor.rank(move.iterator().next());
            if(r>=0 && r<=5 && fullRanks[r]==1)cost-=4.8; // clear a low isolated card
            else if(r>=0 && r<=7 && fullRanks[r]==1)cost-=2.5;
            else if(r>=0 && r<=6)cost-=.6;
        }
        return cost*stage;
    }
    private OpeningTempoPolicy(){}
}
