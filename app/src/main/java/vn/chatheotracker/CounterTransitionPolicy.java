package vn.chatheotracker;

/** Physical counter constraints, independent of the Android frame pipeline.
 * A change from <13 back to 13 is a NEW HAND hypothesis, handled only by
 * RedealDetector when the entire table is corroborated; never a single-seat move. */
final class CounterTransitionPolicy {
    static String rejection(int previous,int candidate){
        if(candidate<1 || candidate>13)return "unreadable_or_missing_badge";
        if(previous<0)return null;  // first verified observation; not a move
        if(candidate>previous)return "unexpected_counter_increase";
        if(previous-candidate>12)return "implausible_move_size";
        return null;
    }
    /** Missing glyph must not silently be reinterpreted as a winner. */
    static boolean mayRecordAsMove(int previous,int next){
        return rejection(previous,next)==null && previous>=1 && next>=1 && next<previous;
    }
    private CounterTransitionPolicy(){}
}
