package vn.chatheotracker;

import java.util.*;

/**
 * Purely OBSERVABLE contexts that MAY be consistent with baiting a 2.
 * Never labels hidden intent or assigns a probability of a chop.
 * A learning model must be evaluated on verified outcomes before using these
 * features as numeric likelihoods.
 */
final class BaitBehaviorSignals {
    static final class Observation {
        final boolean pressureSingle;
        final boolean pressurePair;
        final boolean twoStillInMyHand;
        final int exposedRank,groupSize;
        final int opponentRemaining;
        Observation(boolean a,boolean b,boolean mine,int rank,int size,int remaining){
            pressureSingle=a;pressurePair=b;twoStillInMyHand=mine;
            exposedRank=rank;groupSize=size;opponentRemaining=remaining;
        }
        boolean possibleBaitSignal(){return twoStillInMyHand && (pressureSingle||pressurePair);}
        String label(){return pressureSingle?"high_single_pressure":pressurePair?"high_pair_pressure":"none";}
    }
    static Observation fromConfirmedPlay(Collection<String> play,Collection<String> hand,int opponentRemaining){
        int rank=-1,n=play==null?0:play.size();
        boolean same=true;
        if(play!=null)for(String card:play){
            if(!RecentMoves.isCard(card))return new Observation(false,false,false,-1,n,opponentRemaining);
            int r=MonteCarloAdvisor.rank(card);
            if(rank>=0 && rank!=r)same=false;
            rank=r;
        }
        int twoCount=0;
        if(hand!=null)for(String c:hand)if(RecentMoves.isCard(c)&&MonteCarloAdvisor.rank(c)==12)twoCount++;
        boolean hasTwo=twoCount>0;
        // Only *confirmed* high single (Q/K/A) or pair (K/A) while player
        // still holds enough cards. This does NOT imply the player has chops.
        boolean single=hasTwo && same && n==1 && rank>=9 && rank<=11 && opponentRemaining>=4;
        // A high PAIR can only force a PAIR of 2s; one lone 2 cannot respond.
        boolean pair=twoCount>=2 && same && n==2 && rank>=10 && rank<=11 && opponentRemaining>=4;
        return new Observation(single,pair,hasTwo,rank,n,opponentRemaining);
    }
    private final int[] pressure=new int[3];
    private final int[] highSingle=new int[3];
    private final int[] highPair=new int[3];
    void resetRound(){Arrays.fill(pressure,0);Arrays.fill(highSingle,0);Arrays.fill(highPair,0);}
    Observation record(int seat,Collection<String> play,Collection<String> myHand,int remaining){
        Observation signal=fromConfirmedPlay(play,myHand,remaining);
        if(seat>=0&&seat<3&&signal.possibleBaitSignal()){
            pressure[seat]++;
            if(signal.pressureSingle)highSingle[seat]++;
            if(signal.pressurePair)highPair[seat]++;
        }
        return signal;
    }
    String note(int seat){
        if(seat<0||seat>=3||pressure[seat]<2)return "";
        // At least two distinct confirmed pressure moves this round. A warning
        // to pay attention, NOT evidence of what unseen cards are held.
        return "⚑ áp lực bài cao · chưa rõ có mồi heo";
    }
}
