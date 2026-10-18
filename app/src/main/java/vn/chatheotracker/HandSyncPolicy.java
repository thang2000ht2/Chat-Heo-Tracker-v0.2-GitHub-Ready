package vn.chatheotracker;

/** Only promote a short COMPLETE visual hand when there is evidence the round
 * has progressed. Otherwise eight visible cards could just be an obstructed
 * thirteen-card deal. All accepted scans must additionally be stable in time.
 */
final class HandSyncPolicy {
    static boolean acceptFirstVisualHand(int cards,boolean previousPlaySeen,int[] opponents){
        if(cards<1||cards>13)return false;
        if(cards==13)return true;
        if(!previousPlaySeen && opponents!=null)
            for(int n:opponents)if(n>=0&&n<13)previousPlaySeen=true;
        return previousPlaySeen;
    }
    private HandSyncPolicy(){}
}
