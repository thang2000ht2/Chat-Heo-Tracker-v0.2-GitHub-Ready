package vn.chatheotracker;

/** Display-only safety gate: after a winner has zero cards and the local fan
 * has been absent in eight actual hand scans, do NOT keep presenting the last
 * recorded card as if it were still on the screen. Does not infer a winner,
 * change the card ledger, or label training data. */
final class HandEndVisibility {
    private static final int REQUIRED_MISSING_SCANS=8;
    private int missing;
    void clear(){missing=0;}
    boolean observe(boolean localFanVisible,int[] counts){
        boolean opponentFinished=false;
        if(counts!=null)for(int c:counts)if(c==0)opponentFinished=true;
        if(localFanVisible || !opponentFinished){missing=0;return false;}
        missing=Math.min(REQUIRED_MISSING_SCANS,missing+1);
        return missing>=REQUIRED_MISSING_SCANS;
    }
}
