package vn.chatheotracker;

/** A round can be reset only after a REAL counter cycle: somebody was visibly
 * below thirteen for multiple frames, then all three returned to thirteen.
 * Seeing public cards while 13/13/13 is not progress: those cards may be the
 * local player's opening move or temporary HUD/artifact detections.
 */
final class RedealDetector {
    private static final int REQUIRED_FULL_FRAMES = 7;
    private static final int REQUIRED_LOW_FRAMES = 3;
    private int fullFrames;
    private int lowFrames;
    private boolean sawStableLow;

    void clear(){fullFrames=0;lowFrames=0;sawStableLow=false;}

    /** The legacy roundProgress parameter is retained for caller compatibility,
     * but it cannot arm a new deal without a genuine lower-than-13 badge. */
    boolean observe(int[] rawCounts,boolean roundProgress){
        if(rawCounts==null || rawCounts.length<3){fullFrames=0;lowFrames=0;return false;}
        boolean full=true,low=false;
        for(int i=0;i<3;i++){
            int n=rawCounts[i];
            if(n!=13)full=false;
            if(n>=0 && n<13)low=true;
        }
        if(!full){
            fullFrames=0;
            if(low){
                lowFrames=Math.min(REQUIRED_LOW_FRAMES,lowFrames+1);
                if(lowFrames>=REQUIRED_LOW_FRAMES)sawStableLow=true;
            }else lowFrames=0;
            return false;
        }
        lowFrames=0;
        if(!sawStableLow)return false;
        fullFrames++;
        if(fullFrames<REQUIRED_FULL_FRAMES)return false;
        clear();
        return true;
    }

    /** Used to prevent transitional frames from overwriting the old ledger. */
    boolean awaitingConfirmation(){return sawStableLow && fullFrames>0;}
}
