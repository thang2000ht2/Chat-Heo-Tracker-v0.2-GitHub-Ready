package vn.chatheotracker;

/** Pure-Java detector for the broad white card-fan top edge.  The image recognizer
 * supplies one sampled row at a time; this class does not require Android.
 * The returned X is the measured white-strip origin (the recognizer applies -3px).
 */
final class HandFanGeometry {
    static final class Fan {
        final int left, width, count, row;
        final double error;
        Fan(int l,int w,int n,int y,double e){left=l;width=w;count=n;row=y;error=e;}
    }
    /** Search all white segments in a row rather than relying on only the longest one. */
    static Fan inRow(boolean[] white, int startX, int row){
        Fan best=null;
        int segment=-1;
        for(int i=0;i<=white.length;i++){
            boolean on=i<white.length && white[i];
            if(on && segment<0)segment=i;
            if(!on && segment>=0){
                int length=i-segment;
                int n=(int)Math.round((length-160.0)/94.6)+1;
                if(n>=1 && n<=13){
                    double error=Math.abs(length-(160+(n-1)*94.6));
                    // A broad fan must be geometrically plausible, not just a white button.
                    if(length>=140 && error<=Math.min(38,10+n*2.3)){
                        Fan f=new Fan(startX+segment,length,n,row,error);
                        if(best==null || f.count>best.count ||
                            (f.count==best.count && f.error<best.error))best=f;
                    }
                }
                segment=-1;
            }
        }
        return best;
    }
    private HandFanGeometry(){}
}
