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
        Result(List<String> cards, boolean complete){this(cards,complete,complete?"OK":"CHƯA RÕ");}
        Result(List<String> cards, boolean complete,String diagnosis){
            this.cards=cards;this.complete=complete;this.diagnosis=diagnosis;
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
        if(best==null)return new Result(Collections.emptyList(),false,"KHÔNG THẤY QUẠT");
        // White strip is measured from the fan top. The rank/suit corner boxes
        // are at fixed offsets relative to the start, but allow slight variation.
        Result partial=new Result(Collections.emptyList(),false,"BÀI BỊ CHE / LỆCH GÓC");
        for(int dx:new int[]{0,-3,3,-6,6}){
            for(int dy:new int[]{0,-2,2}){
                int x0=best.left-3+dx;
                int top=best.row-5+dy; // y=715 maps to reference top=710
                ArrayList<String> cards=new ArrayList<>();
                String failure="KHÔNG ĐỌC RÕ GÓC BÀI";
                for(int i=0;i<best.count;i++){
                    int x=x0+(int)Math.round(i*94.6);
                    boolean[][] rank=extract(b,x+9,top+11,x+73,top+67,32,40,true);
                    boolean[][] suit=extract(b,x+8,top+71,x+69,top+129,28,28,false);
                    if(rank==null||suit==null){failure="GÓC BÀI BỊ CHE";break;}
                    Match r=classify(rank,rankTemplates),s=classify(suit,suitTemplates);
                    // Video compression softens the edges. Slightly relaxed thresholds
                    // paired with a unique-card gate and three stable screen frames.
                    if(r.score<0.79 || r.margin<0.022 || s.score<0.84 || s.margin<0.014){
                        failure="MẶT BÀI CHƯA RÕ";break;
                    }
                    String card=r.label+s.label;
                    if(cards.contains(card)){failure="BÀI TRÙNG – QUÉT LẠI";break;}
                    cards.add(card);
                }
                if(cards.size()==best.count)return new Result(cards,true,"OK");
                if(cards.size()>partial.cards.size())partial=new Result(cards,false,failure);
            }
        }
        return partial;
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
