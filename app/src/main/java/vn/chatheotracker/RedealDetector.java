package vn.chatheotracker;

/** Detect the beginning of a fresh deal using successive *raw* counter reads.
 * A previous recorded play OR observed count drop is needed to arm the event;
 * being at 13-13-13 in a new/idle room is never sufficient. */
final class RedealDetector {
    private int stableThirteen=0;
    private boolean armed=false;
    private static final int REQUIRED_FRAMES=7;
    void clear(){stableThirteen=0;armed=false;}
    boolean observe(int[] rawCounts,boolean roundProgress){
        if(roundProgress)armed=true;
        boolean threeThirteens=rawCounts!=null && rawCounts.length>=3 &&
            rawCounts[0]==13 && rawCounts[1]==13 && rawCounts[2]==13;
        if(!threeThirteens){stableThirteen=0;return false;}
        stableThirteen=Math.min(stableThirteen+1,REQUIRED_FRAMES);
        if(armed && stableThirteen>=REQUIRED_FRAMES){clear();return true;}
        return false;
    }
}
