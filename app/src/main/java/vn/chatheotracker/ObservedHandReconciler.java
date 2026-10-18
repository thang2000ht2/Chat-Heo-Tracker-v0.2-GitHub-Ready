package vn.chatheotracker;

import java.util.*;

/** Pure Java: assess a stable visual hand without requiring all opponent
 * counters to be readable. Public cards observed in error can be disputed,
 * but cards are never invented to force the deck total to 52.
 */
final class ObservedHandReconciler {
    static final class Result {
        final boolean feasible;
        final List<String> contradictions;
        final int minimumUnseen;
        final boolean allCountersKnown;
        Result(boolean feasible,List<String> contradictions,int unseen,boolean known) {
            this.feasible=feasible;
            this.contradictions=Collections.unmodifiableList(contradictions);
            this.minimumUnseen=unseen;
            this.allCountersKnown=known;
        }
    }
    static Result inspect(Collection<String> visual,Collection<String> observed,int[] counts) {
        if(visual==null||visual.isEmpty()||observed==null||counts==null||counts.length!=3)
            return new Result(false,new ArrayList<>(),-1,false);
        LinkedHashSet<String> hand=new LinkedHashSet<>(visual),seen=new LinkedHashSet<>(observed);
        if(hand.size()!=visual.size()||seen.size()!=observed.size())
            return new Result(false,new ArrayList<>(),-1,false);
        for(String c:hand)if(!RecentMoves.isCard(c))return new Result(false,new ArrayList<>(),-1,false);
        for(String c:seen)if(!RecentMoves.isCard(c))return new Result(false,new ArrayList<>(),-1,false);
        LinkedHashSet<String> overlaps=new LinkedHashSet<>(hand);
        overlaps.retainAll(seen);
        seen.removeAll(hand);
        int used=hand.size()+seen.size();
        boolean known=true;
        for(int count:counts){
            if(count==-1){known=false;continue;}
            if(count<0 || count>13)return new Result(false,new ArrayList<>(overlaps),-1,false);
            used+=count;
        }
        return new Result(used<=52,new ArrayList<>(overlaps),52-used,known);
    }
    private ObservedHandReconciler(){}
}
