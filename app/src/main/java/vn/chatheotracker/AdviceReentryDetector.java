package vn.chatheotracker;

import java.util.*;

/**
 * A pass-around can return the lead to the local player without a reliable
 * false->true edge from the green action button. Observe confirmed table
 * transitions as a second, independent trigger. Pure Java for replay tests.
 *
 * This does not detect who passed; it merely requests a fresh suggestion when
 * a previously occupied public trick clears while our action is available.
 */
final class AdviceReentryDetector {
    private String previousStable = "";
    private boolean hadOccupiedTrick = false;
    private int emptyFrames = 0;
    private int nonEmptyFrames = 0;
    private long lastTriggered = Long.MIN_VALUE / 4;

    void reset() {
        previousStable="";
        hadOccupiedTrick=false;
        emptyFrames=0;
        nonEmptyFrames=0;
        lastTriggered=Long.MIN_VALUE / 4;
    }

    boolean observe(boolean buttonActive,boolean boardStable,Collection<String> board,long nowMs) {
        // Visibility may briefly drop when cards animate. Require an actual
        // stable table and several successive empty scans, not a blank frame.
        if(!boardStable){emptyFrames=0;nonEmptyFrames=0;return false;}
        if(board!=null && !board.isEmpty()){
            nonEmptyFrames++;
            emptyFrames=0;
            if(nonEmptyFrames>=2){
                hadOccupiedTrick=true;
                ArrayList<String> sorted=new ArrayList<>(board);
                Collections.sort(sorted);
                previousStable=String.join(" ",sorted);
            }
            return false;
        }
        nonEmptyFrames=0;
        emptyFrames++;
        if(!hadOccupiedTrick || !buttonActive || emptyFrames<4 || nowMs-lastTriggered<1500)
            return false;
        // Require previous confirmed nonempty table. A second blank scan is
        // not a new opportunity; the next trigger needs a new occupied trick.
        if(previousStable.isEmpty())return false;
        previousStable="";
        hadOccupiedTrick=false;
        lastTriggered=nowMs;
        return true;
    }
}
