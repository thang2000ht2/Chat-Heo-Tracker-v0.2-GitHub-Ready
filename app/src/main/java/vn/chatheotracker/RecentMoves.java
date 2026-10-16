package vn.chatheotracker;

import java.util.*;

/** Keeps whole plays in reverse chronological order. No rank sorting across plays. */
final class RecentMoves {
    private final ArrayList<LinkedHashSet<String>> newestFirst = new ArrayList<>();

    void record(Collection<String> cards) {
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (String c : cards) if (isCard(c)) unique.add(c);
        if (!unique.isEmpty()) newestFirst.add(0, unique);
    }

    void remove(String card) {
        for (int i = newestFirst.size() - 1; i >= 0; i--) {
            LinkedHashSet<String> move = newestFirst.get(i);
            move.remove(card);
            if (move.isEmpty()) newestFirst.remove(i);
        }
    }

    void clear() { newestFirst.clear(); }
    boolean isEmpty() { return newestFirst.isEmpty(); }
    int size() { return newestFirst.size(); }

    String mostRecent() {
        return newestFirst.isEmpty() ? "" : formatMove(newestFirst.get(0));
    }

    String olderMoves() {
        StringBuilder result = new StringBuilder();
        for (int i = 1; i < newestFirst.size(); i++) {
            if (result.length() > 0) result.append("  ›  ");
            result.append(formatMove(newestFirst.get(i)));
        }
        return result.toString();
    }

    String fullText() {
        if (newestFirst.isEmpty()) return "";
        String older = olderMoves();
        return mostRecent() + (older.isEmpty() ? "" : "  ›  " + older);
    }

    /** Group suits only within the same play; preserve order that cards were observed. */
    private static String formatMove(Collection<String> cards) {
        LinkedHashMap<String, LinkedHashSet<String>> byRank = new LinkedHashMap<>();
        for (String card : cards) {
            if (!isCard(card)) continue;
            String rank = card.substring(0, card.length() - 1);
            byRank.computeIfAbsent(rank, ignored -> new LinkedHashSet<>()).add(card.substring(card.length() - 1));
        }
        StringBuilder out = new StringBuilder();
        for (Map.Entry<String, LinkedHashSet<String>> entry : byRank.entrySet()) {
            if (out.length() > 0) out.append(' ');
            out.append(entry.getKey());
            for (String s : entry.getValue()) out.append(suitSymbol(s));
        }
        return out.toString();
    }

    private static boolean isCard(String s) {
        if (s == null || s.length() < 2) return false;
        String r = s.substring(0, s.length() - 1), suit = s.substring(s.length() - 1);
        return Arrays.asList("3","4","5","6","7","8","9","10","J","Q","K","A","2").contains(r)
                && Arrays.asList("S","C","D","H").contains(suit);
    }

    private static String suitSymbol(String s) {
        switch (s) {
            case "S": return "♠";
            case "C": return "♣";
            case "D": return "♦";
            case "H": return "♥";
            default: return "?";
        }
    }
}
