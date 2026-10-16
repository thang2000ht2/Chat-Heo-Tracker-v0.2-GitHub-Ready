package vn.chatheotracker;

/**
 * Debounced comparison of a *visual* fingerprint of the nickname ribbon.
 * A name-area image can change due to chat/animation, so it is only a best-effort
 * occupant signal: no OCR, no claim of establishing a real-world identity.
 */
final class SeatIdentityTracker {
    enum Event { NONE, FIRST_SEEN, REPLACED, VACATED }
    static final int BITS = 720; // 60 x 12 white-ink cells
    private static final int SAME_DISTANCE = 14;
    private static final int CHANGED_DISTANCE = 20;
    private static final int CLEAR_CHANGE_DISTANCE = 48;
    private static final int CONFIRM_FRAMES = 9;
    private static final int EMPTY_FRAMES = 14;
    private String active, candidate;
    private int candidateFrames, absentFrames;

    SeatIdentityTracker(String previous) {
        active = valid(previous) ? previous : null;
    }
    String active() { return active; }
    static boolean valid(String bits) {
        if (bits == null || bits.length() != BITS) return false;
        int marks = 0;
        for (int i = 0; i < bits.length(); i++) {
            char c = bits.charAt(i);
            if (c != '0' && c != '1') return false;
            if (c == '1') marks++;
        }
        // Sparse blank ribbons and fully covered labels cannot identify a person.
        return marks >= 24 && marks <= 490;
    }
    static int distance(String a, String b) {
        if (!valid(a) || !valid(b)) return BITS;
        int differing = 0;
        for (int i = 0; i < BITS; i++) if (a.charAt(i) != b.charAt(i)) differing++;
        return differing;
    }
    Event observe(String measured, boolean roomVisible) {
        if (!valid(measured)) {
            candidate = null;candidateFrames = 0;
            if (!roomVisible) absentFrames = 0;
            if (active != null && roomVisible && ++absentFrames >= EMPTY_FRAMES) {
                active = null;absentFrames = 0;
                return Event.VACATED;
            }
            return Event.NONE;
        }
        absentFrames = 0;
        if (active != null) {
            int diff = distance(active, measured);
            if (diff <= SAME_DISTANCE) {
                candidate = null;candidateFrames = 0;
                return Event.NONE;
            }
            // If only a small part differs, avoid replacing a player due to animation.
            if (diff < CHANGED_DISTANCE) {
                candidate = null;candidateFrames = 0;
                return Event.NONE;
            }
        }
        if (candidate != null && distance(candidate, measured) <= SAME_DISTANCE)
            candidateFrames++;
        else { candidate = measured; candidateFrames = 1; }
        // Similar-looking nicknames need longer confirmation than a clear change.
        int need = active != null && distance(active, measured) < CLEAR_CHANGE_DISTANCE
            ? CONFIRM_FRAMES + 3 : CONFIRM_FRAMES;
        if (candidateFrames < need) return Event.NONE;
        Event event = active == null ? Event.FIRST_SEEN : Event.REPLACED;
        active = candidate;
        candidate = null;candidateFrames = 0;
        return event;
    }
}
