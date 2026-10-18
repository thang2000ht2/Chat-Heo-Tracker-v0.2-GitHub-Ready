package vn.chatheotracker;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import java.io.InputStream;
import java.util.*;

/** Conservative recognition of the local thirteen-card fan on the 2048x942 landscape canvas.
 * Never reads opponents' hidden cards. A partial/occluded hand is reported as unknown.
 * Templates were learned from the user's provided screenshots, not a general-purpose ML model.
 */
final class HandRecognizer {
    private static final String[] RANKS={"3","4","5","6","7","8","9","10","J","Q","K","A","2"};
    private static final String[] SUITS={"S","C","D","H"};
    private final Map<String,boolean[][]> rankTemplates=new LinkedHashMap<>();
    private final Map<String,boolean[][]> suitTemplates=new LinkedHashMap<>();

    HandRecognizer(Context ctx) {
        for(String rank:RANKS)load(ctx,"hand_rank_"+rank+".png",rank,rankTemplates);
        for(String suit:SUITS)load(ctx,"hand_suit_"+suit+".png",suit,suitTemplates);
    }
    private void load(Context ctx,String path,String key,Map<String,boolean[][]> into) {
        try(InputStream input=ctx.getAssets().open(path)) {
            Bitmap b=BitmapFactory.decodeStream(input);
            if(b!=null){
                boolean[][] pixels=new boolean[b.getHeight()][b.getWidth()];
                for(int y=0;y<b.getHeight();y++)for(int x=0;x<b.getWidth();x++)
                    pixels[y][x]=Color.red(b.getPixel(x,y))>120;
                into.put(key,pixels);
                b.recycle();
            }
        }catch(Exception e){android.util.Log.w("TrackerHand","Template missing "+path,e);}
    }
    static final class Result {
        final List<String> cards;
        final boolean complete;
        final String diagnosis;
        final List<String> slots;
        final int fanCount, fanStart;
        Result(List<String> cards, boolean complete){this(cards,complete,complete?"OK":"CHƯA RÕ");}
        Result(List<String> cards, boolean complete,String diagnosis){
            this(cards,complete,diagnosis,Collections.emptyList(),0,-1);
        }
        Result(List<String> cards, boolean complete,String diagnosis,List<String> slots,int count,int start){
            this.cards=cards;this.complete=complete;this.diagnosis=diagnosis;
            this.slots=slots;this.fanCount=count;this.fanStart=start;
        }
    }
    /** Full initial hand and later short hands share a single geometry search.
     * Scan multiple near-edge rows to tolerate small shifts and game UI scaling.
     * Conservative classification and the caller's multi-frame confirmation remain required.
     */
    Result recognizeFullHand(Bitmap b){
        Result r=recognizeVisibleHand(b);
        return r.complete && r.cards.size()==13?r:
            new Result(r.cards,false,r.complete?"ĐANG ĐỌC TAY BÀI":"THIẾU 13 · "+r.diagnosis);
    }
    Result recognizeVisibleHand(Bitmap b){
        if(b.getWidth()!=2048 || b.getHeight()!=942 || rankTemplates.size()!=13 || suitTemplates.size()!=4)
            return new Result(Collections.emptyList(),false,"SAI KHUNG / THIẾU MẪU");
        HandFanGeometry.Fan best=null;
        for(int y=706;y<=723;y+=2){
            boolean[] strip=new boolean[1471];
            for(int x=350;x<=1820;x++){
                int rgb=b.getPixel(x,y);
                strip[x-350]=Color.red(rgb)>202&&Color.green(rgb)>202&&Color.blue(rgb)>202;
            }
            HandFanGeometry.Fan found=HandFanGeometry.inRow(strip,350,y);
            if(found==null)continue;
            // Prefer 13 full cards over a partially hidden row. If all rows see the
            // same number, prefer the row at the top of the fan (not suit artwork).
            if(best==null || found.count>best.count ||
                (found.count==best.count && found.error<best.error))best=found;
        }
        if(best==null){
            // Slow fallback only when the normal fast path finds nothing. On some
            // devices the cards are shifted vertically after an animation/OS inset.
            // A candidate is still required to pass per-corner template matching;
            // this expands the search, never manufactures a missing card.
            for(int y=668;y<=760;y+=4){
                if(y>=706&&y<=723)continue;
                boolean[] strip=new boolean[1471];
                for(int x=350;x<=1820;x++){
                    int c=b.getPixel(x,y);
                    strip[x-350]=Color.red(c)>202&&Color.green(c)>202&&Color.blue(c)>202;
                }
                HandFanGeometry.Fan found=HandFanGeometry.inRow(strip,350,y);
                if(found!=null && (best==null || found.count>best.count ||
                    (found.count==best.count && found.error<best.error)))best=found;
            }
        }
        // A raised 9/10/J can break the white strip into separate card tops.
        // Infer card count from several white left edges, but do not classify
        // any card until its rank AND suit templates pass the confidence gates.
        HandFanGeometry.Fan raised=findRaisedFan(b);
        if(raised!=null && (best==null || raised.count>best.count))best=raised;
        if(best==null)return new Result(Collections.emptyList(),false,"KHÔNG THẤY QUẠT");
        // White strip is measured from the fan top. The rank/suit corner boxes
        // are at fixed offsets relative to the start, but allow slight variation.
        Result partial=new Result(Collections.emptyList(),false,"BÀI BỊ CHE / LỆCH GÓC");
        for(int dx:new int[]{0,-3,3,-6,6}){
            for(int dy:new int[]{0,-2,2}){
                int x0=best.left-3+dx;
                int top=best.row-5+dy;
                ArrayList<String> visible=new ArrayList<>();
                ArrayList<String> slots=new ArrayList<>();
                String failure="KHÔNG ĐỌC RÕ GÓC BÀI";
                for(int i=0;i<best.count;i++){
                    String card=null;
                    int x=x0+(int)Math.round(i*94.6);
                    boolean[][] rank=extract(b,x+9,top+11,x+73,top+67,32,40,true);
                    boolean[][] suit=extract(b,x+8,top+71,x+69,top+129,28,28,false);
                    if(rank==null || suit==null)failure="GÓC BÀI BỊ CHE";
                    else{
                        Match r=classify(rank,rankTemplates),st=classify(suit,suitTemplates);
                        if(r.score>=.79 && r.margin>=.022 && st.score>=.84 && st.margin>=.014){
                            String candidate=r.label+st.label;
                            if(!visible.contains(candidate)){card=candidate;visible.add(card);}
                            else failure="BÀI TRÙNG – QUÉT LẠI";
                        }else failure="MẶT BÀI CHƯA RÕ";
                    }
                    slots.add(card);
                }
                if(visible.size()==best.count)
                    return new Result(visible,true,"OK",slots,best.count,best.left);
                if(visible.size()>partial.cards.size())
                    partial=new Result(visible,false,failure,slots,best.count,best.left);
            }
        }
        // The global y-offset is not enough when individual cards are selected
        // (raised) while their neighbours remain at the bottom of the screen.
        // Use measured white-top anchors and independently classify each slot.
        Result raisedResult=recognizeRaisedCorners(b,best);
        if(raisedResult.complete || raisedResult.cards.size()>partial.cards.size())return raisedResult;
        return partial;
    }

    /** Detect a fan from top-edge segments on multiple rows; selected cards may
     * have a 20-40px higher top edge than the adjacent cards.
     */
    private static HandFanGeometry.Fan findRaisedFan(Bitmap bitmap){
        ArrayList<Integer> edges=new ArrayList<>();
        for(int y=678;y<=751;y+=3){
            int start=-1;
            for(int x=350;x<=1821;x++){
                boolean white=false;
                if(x<=1820){
                    int c=bitmap.getPixel(x,y);
                    white=Color.red(c)>202 && Color.green(c)>202 && Color.blue(c)>202;
                }
                if(white && start<0)start=x;
                else if(!white && start>=0){
                    int len=x-start;
                    if(len>=74 && len<=210)edges.add(start);
                    start=-1;
                }
            }
        }
        return HandFanGeometry.fromWhiteEdges(edges,714);
    }

    /** Earliest bright rectangle top for this specific slot. A selected card
     * will rise without moving the other slots, so this must be per-card.
     */
    private static int firstWhiteTop(Bitmap b,int x){
        for(int y=672;y<=755;y++){
            int white=0;
            for(int xx=x+12;xx<=x+79;xx+=2){
                if(xx<0||xx>=b.getWidth())continue;
                int c=b.getPixel(xx,y);
                if(Color.red(c)>202 && Color.green(c)>202 && Color.blue(c)>202)white++;
            }
            if(white>=19)return y;
        }
        return -1;
    }
    private Result recognizeRaisedCorners(Bitmap b,HandFanGeometry.Fan fan){
        if(fan==null || fan.count<=0)return new Result(Collections.emptyList(),false,"KHÔNG THẤY QUẠT");
        ArrayList<String> visible=new ArrayList<>(),slots=new ArrayList<>();
        for(int i=0;i<fan.count;i++){
            int x=fan.left-3+(int)Math.round(i*94.6);
            int whiteTop=firstWhiteTop(b,x);
            String bestCard=null;
            double bestScore=-1;
            if(whiteTop>=0){
                // Include +30px: a lower card can be partly covered by an
                // earlier raised one (e.g. the rightmost K in screenshot 1).
                for(int dy:new int[]{0,-5,5,10,15,20,25,30,35,40}){
                    int top=whiteTop-5+dy;
                    for(int dx:new int[]{-5,0,3,6}){
                        boolean[][] rank=extract(b,x+dx+9,top+11,x+dx+73,top+67,32,40,true);
                        boolean[][] suit=extract(b,x+dx+8,top+71,x+dx+69,top+129,28,28,false);
                        if(rank==null || suit==null)continue;
                        Match r=classify(rank,rankTemplates),st=classify(suit,suitTemplates);
                        if(r.score>=.79 && r.margin>=.022 && st.score>=.84 && st.margin>=.014){
                            double quality=r.score*st.score;
                            if(quality>bestScore){bestScore=quality;bestCard=r.label+st.label;}
                        }
                    }
                }
            }
            if(bestCard!=null && visible.contains(bestCard))bestCard=null;
            slots.add(bestCard);
            if(bestCard!=null)visible.add(bestCard);
        }
        return new Result(visible,visible.size()==fan.count,
            visible.size()==fan.count?"GÓC BÀI LINH HOẠT":"GÓC "+visible.size()+"/"+fan.count,
            slots,fan.count,fan.left);
    }
    private static boolean ink(int a) {
        int r=Color.red(a),g=Color.green(a),b=Color.blue(a);
        return (r>90 && r>g*1.4 && r>b*1.4) || (r<120 && g<120 && b<120);
    }
    /** Connected components; crop only meaningful ink on the left card corner. */
    private static boolean[][] extract(Bitmap b,int x0,int y0,int x1,int y1,int outW,int outH,boolean isRank){
        int w=x1-x0,h=y1-y0;
        if(x0<0||y0<0||x1>b.getWidth()||y1>b.getHeight())return null;
        boolean[][] black=new boolean[h][w],visited=new boolean[h][w],accepted=new boolean[h][w];
        for(int y=0;y<h;y++)for(int x=0;x<w;x++)black[y][x]=ink(b.getPixel(x0+x,y0+y));
        int[] queue=new int[w*h];
        for(int sy=0;sy<h;sy++)for(int sx=0;sx<w;sx++){
            if(!black[sy][sx]||visited[sy][sx])continue;
            int front=0,back=0;
            queue[back++]=sy*w+sx;visited[sy][sx]=true;
            int minX=sx,maxX=sx,minY=sy,maxY=sy;
            while(front<back){
                int p=queue[front++],xx=p%w,yy=p/w;
                minX=Math.min(minX,xx);maxX=Math.max(maxX,xx);
                minY=Math.min(minY,yy);maxY=Math.max(maxY,yy);
                for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++){
                    int nx=xx+dx,ny=yy+dy;
                    if(nx>=0&&nx<w&&ny>=0&&ny<h&&!visited[ny][nx]&&black[ny][nx]){
                        visited[ny][nx]=true;queue[back++]=ny*w+nx;
                    }
                }
            }
            if(back<35||maxY-minY+1<8||maxX-minX+1<3)continue;
            if(minX>(isRank?47:41)||minY>(isRank?43:44))continue;
            for(int i=0;i<back;i++){int p=queue[i];accepted[p/w][p%w]=true;}
        }
        int minX=w,minY=h,maxX=-1,maxY=-1;
        for(int yy=0;yy<h;yy++)for(int xx=0;xx<w;xx++)if(accepted[yy][xx]){
            minX=Math.min(minX,xx);maxX=Math.max(maxX,xx);
            minY=Math.min(minY,yy);maxY=Math.max(maxY,yy);
        }
        if(maxX<0)return null;
        int sw=maxX-minX+1,sh=maxY-minY+1;
        boolean[][] result=new boolean[outH][outW];
        for(int yy=0;yy<outH;yy++)for(int xx=0;xx<outW;xx++)
            result[yy][xx]=accepted[minY+Math.min(sh-1,yy*sh/outH)][minX+Math.min(sw-1,xx*sw/outW)];
        return result;
    }
    private static final class Match{
        final String label;final double score,margin;
        Match(String l,double s,double m){label=l;score=s;margin=m;}
    }
    private static Match classify(boolean[][] sample,Map<String,boolean[][]> templates){
        String winner="";double top=-1,second=-1;
        for(Map.Entry<String,boolean[][]> e:templates.entrySet()){
            boolean[][] t=e.getValue();int common=0,a=0,b=0;
            for(int y=0;y<sample.length;y++)for(int x=0;x<sample[0].length;x++){
                if(sample[y][x])a++;
                if(t[y][x])b++;
                if(sample[y][x]&&t[y][x])common++;
            }
            double sim=2.0*common/Math.max(1,a+b);
            if(sim>top){second=top;top=sim;winner=e.getKey();}
            else if(sim>second)second=sim;
        }
        return new Match(winner,top,top-Math.max(0,second));
    }
}
