package vn.chatheotracker;

import java.util.*;

/** A player-confirmed pass against the publicly visible lead.
 * Pass is soft evidence only: a player may choose not to beat even when able.
 * A record becomes stale after the player sheds more cards.
 */
final class ObservedPass {
    final List<String> lead;
    final int countAtPass;
    ObservedPass(Collection<String> board,int count){
        lead=Collections.unmodifiableList(new ArrayList<>(board));
        countAtPass=count;
    }
    /** Return 1.0 for no evidence; shrink a witnessed pass against hands that
     * can beat the lead. Never hard-reject those hands.
     */
    double likelihood(Collection<String> hypotheticalHand,int currentCount){
        if(lead.isEmpty() || hypotheticalHand==null)return 1.0;
        if(currentCount<0 || countAtPass<0 || currentCount>countAtPass)return 1.0;
        boolean reply=canBeat(hypotheticalHand,lead);
        if(!reply)return 1.0;
        // We only know the *current* cards, not necessarily the exact hand at pass.
        // Older passes receive far less weight after subsequent plays.
        int playedSince=countAtPass-currentCount;
        double evidence=playedSince==0?.32:playedSince<=2?.12:.04;
        return 1.0-evidence;
    }
    /** Deliberately incomplete reference rules. Ignore unsupported trick types. */
    static boolean canBeat(Collection<String> hand,Collection<String> lead){
        if(hand==null||lead==null||lead.isEmpty())return false;
        int[] have=new int[13],topSuit=new int[13],onTable=new int[13];
        Arrays.fill(topSuit,-1);
        for(String c:hand){int r=MonteCarloAdvisor.rank(c);if(r<0)return false;
            have[r]++;topSuit[r]=Math.max(topSuit[r],MonteCarloAdvisor.suit(c));}
        for(String c:lead){int r=MonteCarloAdvisor.rank(c);if(r<0)return false;onTable[r]++;}
        if(lead.size()==1){
            String card=lead.iterator().next();int r=MonteCarloAdvisor.rank(card);
            for(int a=r;a<13;a++){
                if(a>r && have[a]>0)return true;
                if(a==r && have[a]>0 && topSuit[a]>MonteCarloAdvisor.suit(card))return true;
            }
            // Some Pig Hunters rules allow special combos over a 2.
            if(r==12){
                for(int n:have)if(n==4)return true;
                for(int s=0;s<=9;s++)if(have[s]>=2&&have[s+1]>=2&&have[s+2]>=2)return true;
            }
            return false;
        }
        int count=lead.size(),r=-1;
        for(int i=0;i<13;i++)if(onTable[i]>0){
            if(onTable[i]!=count)return false;
            r=i;break;
        }
        if(r<0)return false;
        if(count<2||count>4)return false;
        for(int i=r+1;i<13;i++)if(have[i]>=count)return true;
        return false;
    }
}
