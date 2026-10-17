package vn.chatheotracker;

import java.util.*;

/** Separate an impossible deck from one where a few played cards were missed.
 * Missing public observations are NOT silently credited to any opponent.
 */
final class PartialDeckState {
    static final class Result {
        final boolean feasible;final boolean exact;final int missing;
        Result(boolean f,boolean e,int n){feasible=f;exact=e;missing=n;}
    }
    static Result check(Collection<String> hand,Collection<String> played,int[] opponents){
        if(hand==null||hand.isEmpty()||played==null||opponents==null||opponents.length!=3)
            return new Result(false,false,-1);
        HashSet<String> h=new HashSet<>(hand),p=new HashSet<>(played);
        if(h.size()!=hand.size()||p.size()!=played.size())return new Result(false,false,-1);
        for(String c:h)if(p.contains(c)||MonteCarloAdvisor.rank(c)<0||MonteCarloAdvisor.suit(c)<0)
            return new Result(false,false,-1);
        for(String c:p)if(MonteCarloAdvisor.rank(c)<0||MonteCarloAdvisor.suit(c)<0)
            return new Result(false,false,-1);
        int used=h.size()+p.size();
        for(int count:opponents){if(count<0||count>13)return new Result(false,false,-1);used+=count;}
        int missing=52-used;
        return new Result(missing>=0,missing==0,missing);
    }
}
