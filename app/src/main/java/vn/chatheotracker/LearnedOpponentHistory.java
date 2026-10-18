package vn.chatheotracker;

import java.util.*;

/**
 * Small bounded, on-device record of *confirmed* observed moves for one seat's
 * current occupant. This is adaptive history, not a pretrained AI model.
 * Does not contain screenshots, account names or unknown hidden cards.
 */
final class LearnedOpponentHistory {
    static final int LIMIT = 100;
    private final ArrayDeque<List<String>> moves = new ArrayDeque<>();

    void record(Collection<String> played) {
        if (played == null) return;
        LinkedHashSet<String> legal = new LinkedHashSet<>();
        for (String card : played) if (RecentMoves.isCard(card)) legal.add(card);
        if (legal.isEmpty() || legal.size() > 13) return;
        moves.addLast(new ArrayList<>(legal));
        while (moves.size() > LIMIT) moves.removeFirst();
    }
    /** Explicit new-hand separator avoids treating singles from separate deals as a broken pair. */
    void newRound() {
        if (!moves.isEmpty() && !moves.peekLast().isEmpty()) {
            moves.addLast(Collections.emptyList());
            while (moves.size() > LIMIT) moves.removeFirst();
        }
    }
    /** Retract misrecognized card identities only from THIS round's samples.
     * Keep legitimate identically-named cards from previous deals intact.
     * This is invoked only after a repeatedly verified visual-hand contradiction.
     */
    int retractCurrentRound(Collection<String> disputed){
        if(disputed==null||disputed.isEmpty())return 0;
        HashSet<String> invalid=new HashSet<>(disputed);
        int changed=0;
        Iterator<List<String>> it=moves.descendingIterator();
        while(it.hasNext()){
            List<String> play=it.next();
            if(play.isEmpty())break; // End of current round.
            if(play.removeAll(invalid)){
                changed++;
                if(play.isEmpty())it.remove();
            }
        }
        return changed;
    }
    void clear() { moves.clear(); }
    int size() { return moves.size(); }
    List<List<String>> snapshot() {
        List<List<String>> result = new ArrayList<>();
        for (List<String> move : moves) result.add(new ArrayList<>(move));
        return result;
    }
    String encode() {
        StringBuilder out = new StringBuilder("v1|");
        for (List<String> move : moves) {
            if (out.length() > 3) out.append(';');
            if (move.isEmpty()) { out.append('#'); continue; }
            for (int i = 0; i < move.size(); i++) {
                if (i > 0) out.append(',');
                out.append(move.get(i));
            }
        }
        return out.toString();
    }
    static LearnedOpponentHistory decode(String encoded) {
        LearnedOpponentHistory history = new LearnedOpponentHistory();
        if (encoded == null || !encoded.startsWith("v1|")) return history;
        String tail = encoded.substring(3);
        if (tail.isEmpty() || tail.length() > 12000) return history;
        for (String play : tail.split(";")) {
            if ("#".equals(play)) { history.newRound(); continue; }
            String[] cards = play.split(",");
            if (cards.length < 1 || cards.length > 13) continue;
            boolean valid = true;
            for (String card : cards) if (!RecentMoves.isCard(card)) valid = false;
            if (valid && new HashSet<>(Arrays.asList(cards)).size() == cards.length)
                history.record(Arrays.asList(cards));
        }
        return history;
    }
}
