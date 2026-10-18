package vn.chatheotracker;

import java.util.*;

/** Separates an active white-layer play from gray-backed historical cards.
 * Gray cards are valuable for display diagnostics, but they MUST NOT create
 * a new played-card event or be credited to a different opponent.
 */
final class ActiveBoardEvidence {
    static LinkedHashSet<String> active(Collection<String> combined,
                                        Collection<String> white){
        LinkedHashSet<String> result=new LinkedHashSet<>();
        if(white==null)return result;
        for(String c:white)if(c!=null && combined!=null && combined.contains(c))result.add(c);
        // The active classifier is itself a positive detector; if combined
        // missed the card, do not discard direct active-layer evidence.
        for(String c:white)if(c!=null)result.add(c);
        return result;
    }
    private ActiveBoardEvidence(){}
}
