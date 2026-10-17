package vn.chatheotracker;

import java.util.*;

/** Independent of advice/deck accounting. Never infer opponents' hidden cards as fact. */
final class HeoWarningAdvisor {
    static String warning(Collection<String> myHand,Collection<String> observed){
        if(myHand==null||myHand.isEmpty()||observed==null)return "";
        boolean myTwo=false;
        for(String card:myHand)if(card.startsWith("2"))myTwo=true;
        if(!myTwo)return "";
        HashSet<String> unseen=new HashSet<>();
        for(String r:MonteCarloAdvisor.RANKS)for(String s:MonteCarloAdvisor.SUITS){
            String c=r+s;
            if(!myHand.contains(c)&&!observed.contains(c))unseen.add(c);
        }
        int[] remainingByRank=new int[13];
        for(String card:unseen)remainingByRank[MonteCarloAdvisor.rank(card)]++;
        boolean possibleFour=false,possiblePairs=false;
        for(int n:remainingByRank)if(n==4)possibleFour=true;
        for(int start=0;start<10;start++){
            if(remainingByRank[start]>=2&&remainingByRank[start+1]>=2&&remainingByRank[start+2]>=2){
                possiblePairs=true;break;
            }
        }
        return (possibleFour||possiblePairs)?"⚠ HEO: chưa loại trừ khả năng bị chặt":"";
    }
    private HeoWarningAdvisor(){}
}
