package vn.chatheotracker;

import java.util.*;

/** Detect the local player's opening/continuing play using two INDEPENDENT
 * observations: verified hand shrinkage and the same card identities seen on
 * the table. Opponent badges need not decrease when we lead the round.
 * Does not guess cards missing due to a temporary hand occlusion. */
final class LocalPlayEvidence {
    static List<String> confirmed(Collection<String> oldHand, Collection<String> visibleHand,
                                  Collection<String> boardSeen){
        if(oldHand==null||visibleHand==null||boardSeen==null)return Collections.emptyList();
        LinkedHashSet<String> previous=new LinkedHashSet<>(oldHand);
        LinkedHashSet<String> current=new LinkedHashSet<>(visibleHand);
        LinkedHashSet<String> board=new LinkedHashSet<>(boardSeen);
        if(previous.size()!=oldHand.size() || current.size()!=visibleHand.size() ||
            current.isEmpty() || current.size()>=previous.size() ||
            !previous.containsAll(current))return Collections.emptyList();
        previous.removeAll(current);
        if(!board.containsAll(previous))return Collections.emptyList();
        return new ArrayList<>(previous);
    }
    /** Conservative opening inference when capture missed the original 13:
     * with three verified opponent 13s, 9 visible local cards plus four active
     * white public cards must account for all thirteen of the local player's
     * original cards. Never use grayed historic cards as the opening evidence. */
    static List<String> opening(Collection<String> visibleHand,
                                Collection<String> activeWhiteBoard,int[] counts){
        if(visibleHand==null||activeWhiteBoard==null||counts==null||counts.length!=3)
            return Collections.emptyList();
        for(int n:counts)if(n!=13)return Collections.emptyList();
        LinkedHashSet<String> current=new LinkedHashSet<>(visibleHand);
        LinkedHashSet<String> table=new LinkedHashSet<>(activeWhiteBoard);
        if(current.size()!=visibleHand.size() || table.size()!=activeWhiteBoard.size() ||
            current.isEmpty() || table.isEmpty() || current.size()+table.size()!=13)
            return Collections.emptyList();
        for(String c:table)if(current.contains(c))return Collections.emptyList();
        return new ArrayList<>(table);
    }
    private LocalPlayEvidence(){}
}
