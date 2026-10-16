package vn.chatheotracker;

import java.util.*;

/**
 * Interpretable behavioral *heuristics*, not trained or calibrated probabilities.
 * Public information only: chronological move groups and opponents' card counts.
 * Cannot know passes or hidden combinations unless separately observed.
 */
final class OpponentBehaviorModel {
    static final class Profile {
        final int singles, pairs, triples, fours, brokenPairs, moves;
        Profile(List<? extends Collection<String>> history) {
            int one=0,two=0,three=0,four=0,broken=0,total=0;
            HashMap<String,Integer> previouslySingle=new HashMap<>();
            if(history!=null)for(Collection<String> move:history){
                if(move==null||move.isEmpty())continue;
                ArrayList<String> c=new ArrayList<>();
                for(String s:move)if(RecentMoves.isCard(s))c.add(s);
                if(c.isEmpty())continue;
                total++;
                HashMap<String,Integer> rankCounts=new HashMap<>();
                for(String s:c){String rank=s.substring(0,s.length()-1);
                    rankCounts.put(rank,rankCounts.getOrDefault(rank,0)+1);
                }
                if(c.size()==1){
                    one++;
                    String rank=c.get(0).substring(0,c.get(0).length()-1);
                    if(previouslySingle.getOrDefault(rank,0)>0)broken++;
                    previouslySingle.put(rank,previouslySingle.getOrDefault(rank,0)+1);
                } else if(c.size()==2 && rankCounts.size()==1)two++;
                else if(c.size()==3 && rankCounts.size()==1)three++;
                else if(c.size()==4 && rankCounts.size()==1)four++;
            }
            singles=one;pairs=two;triples=three;fours=four;brokenPairs=broken;moves=total;
        }
        double breakPairWillingness(){
            // Two singles of same rank on different turns prove a pair was split.
            // Playing actual pairs adds only a weak preference to preserve other pairs.
            double p=0.28 + 0.19*Math.min(2,brokenPairs) - 0.025*Math.min(4,pairs);
            return clamp(p,0.15,0.83);
        }
    }
    static Profile[] fromHistory(List<List<List<String>>> histories){
        Profile[] profiles=new Profile[3];
        for(int i=0;i<3;i++)profiles[i]=new Profile(histories==null||i>=histories.size()?null:histories.get(i));
        return profiles;
    }
    static double singleReplyChance(Collection<String> opponentHand, String attack, int remaining, Profile p){
        int attackRank=MonteCarloAdvisor.rank(attack),attackSuit=MonteCarloAdvisor.suit(attack);
        if(attackRank<0||attackSuit<0)return 0;
        int[] ranks=new int[13];
        ArrayList<String> candidates=new ArrayList<>();
        for(String c:opponentHand){int r=MonteCarloAdvisor.rank(c);if(r>=0)ranks[r]++;}
        for(String c:opponentHand){
            int r=MonteCarloAdvisor.rank(c),s=MonteCarloAdvisor.suit(c);
            if(r>attackRank || (r==attackRank&&s>attackSuit))candidates.add(c);
        }
        if(candidates.isEmpty())return 0;
        double stay=1;
        for(String card:candidates){
            int r=MonteCarloAdvisor.rank(card);
            // Retain a pair/triple when it is expensive to split just to overtake a single.
            // A lone response that breaks a pair is plausible. Splitting a triple
            // or a four-of-a-kind is generally costlier, but never impossible.
            double willingness = ranks[r]>=2 ? p.breakPairWillingness() : .70;
            if(ranks[r]==3)willingness*=.66;
            else if(ranks[r]==4)willingness*=.39;
            if(r==12)willingness*=.47; // Preserve heo for control / avoid chop.
            else if(r>=10)willingness*=.84;
            // Close to finishing, a player is more willing to break protected combinations.
            if(remaining<=3)willingness=Math.min(.97,willingness+0.35);
            else if(remaining<=5)willingness=Math.min(.93,willingness+0.18);
            // Higher required rank than the attack makes voluntary use of control cards rarer.
            int distance=r-attackRank;
            if(distance>=4 && remaining>4)willingness*=.87;
            stay*=1-clamp(willingness,.02,.97);
        }
        return clamp(1-stay,0,1);
    }
    static double groupReplyChance(int[] rankCounts,String kind,int size,int highRank,int opponentRemaining,Profile p){
        // Caller checks whether a legally stronger combination exists.
        double base=kind.equals("pair")?.63:kind.equals("triple")?.66:
            kind.equals("four")?.60:kind.startsWith("pairs")?.52:.58;
        // Only visible plays influence this adjustment: no invented passes or hands.
        if(kind.equals("pair")&&p.pairs>=2)base+=.10;
        if(kind.equals("triple")&&p.triples>0)base+=.07;
        if(kind.equals("four")&&p.fours>0)base+=.10;
        if(opponentRemaining<=3)base+=.25;
        else if(opponentRemaining<=5)base+=.14;
        if(highRank>=10&&opponentRemaining>5)base-=.09;
        return clamp(base,.20,.96);
    }
    static double chopWillingness(int remaining,Profile p){
        double x=.71;
        if(remaining<=4)x+=.16;
        if(p.pairs>=2)x+=.03;
        if(p.fours>0)x+=.09;
        if(p.brokenPairs>0)x+=.03;
        return clamp(x,.35,.97);
    }
    private static double clamp(double x,double a,double b){return Math.max(a,Math.min(b,x));}
    private OpponentBehaviorModel(){}
}
