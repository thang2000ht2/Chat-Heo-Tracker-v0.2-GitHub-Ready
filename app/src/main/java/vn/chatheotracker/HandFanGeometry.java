package vn.chatheotracker;

import java.util.*;

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
    /** Recover a fan whose raised cards split the single horizontal white strip.
     * Samples contain LEFT edges of 75-210px white sections across several rows.
     * Require at least three DISTINCT lattice positions with 94.6px spacing:
     * one isolated white card or text box must not fabricate a hand.
     */
    static Fan fromWhiteEdges(Collection<Integer> edgeXs, int row) {
        if(edgeXs==null || edgeXs.size()<3)return null;
        ArrayList<Integer> edges=new ArrayList<>(edgeXs);
        Collections.sort(edges);
        Fan best=null;double bestScore=Double.NEGATIVE_INFINITY;
        for(int anchor:edges){
            if(anchor<350 || anchor>1720)continue;
            boolean[] slots=new boolean[13];
            double[] nearest=new double[13];
            Arrays.fill(nearest,Double.POSITIVE_INFINITY);
            for(int x:edges){
                int pos=(int)Math.round((x-anchor)/94.6);
                if(pos<0 || pos>=13)continue;
                double delta=Math.abs(x-(anchor+pos*94.6));
                if(delta<=11 && delta<nearest[pos]){
                    nearest[pos]=delta;slots[pos]=true;
                }
            }
            int distinct=0,max=-1,miss=0;double error=0;
            for(int i=0;i<13;i++)if(slots[i]){
                distinct++;max=i;error+=nearest[i];
            }
            if(distinct<3)continue;
            for(int i=0;i<=max;i++)if(!slots[i])miss++;
            if(miss>2)continue;
            // A complete spread is preferred over isolated false white objects.
            double score=distinct*8.0-miss*5.0-error*.26;
            if(score>bestScore || (score==bestScore && (best==null || max+1>best.count))){
                bestScore=score;
                best=new Fan(anchor,(int)Math.round(160+94.6*max),max+1,row,error);
            }
        }
        return best;
    }
    private HandFanGeometry(){}
}
