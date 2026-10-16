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
        Result(List<String> cards, boolean complete){this.cards=cards;this.complete=complete;}
    }
    /** Requires a full, 13-card visible fan with high-confidence individual rank/suit matches. */
    Result recognizeFullHand(Bitmap b){
        if(b.getWidth()!=2048||b.getHeight()!=942)return new Result(Collections.emptyList(),false);
        // A continuous bright strip near y=715 is the card fan, unlike dark table/chat.
        int first=-1,last=-1,bestLen=0,start=-1;
        final int y=715;
        for(int x=350;x<=1820;x++){
            int color=b.getPixel(x,y);
            boolean white=Color.red(color)>205&&Color.green(color)>205&&Color.blue(color)>205;
            if(white&&start<0)start=x;
            if((!white||x==1820)&&start>=0){
                int end=white?x+1:x;
                if(end-start>bestLen){first=start;last=end;bestLen=end-start;}
                start=-1;
            }
        }
        // For 13 fanned cards the strip is ~1,295px wide. Do not guess on shorter hands.
        if(bestLen<1250||bestLen>1355||rankTemplates.size()!=13||suitTemplates.size()!=4)
            return new Result(Collections.emptyList(),false);
        final int x0=first-3, top=710;
        ArrayList<String> cards=new ArrayList<>();
        for(int index=0;index<13;index++){
            int x=x0+(int)Math.round(index*94.6);
            boolean[][] r=extract(b,x+9,top+11,x+73,top+67,32,40,true);
            boolean[][] s=extract(b,x+8,top+71,x+69,top+129,28,28,false);
            if(r==null||s==null)return new Result(cards,false);
            Match rank=classify(r,rankTemplates),suit=classify(s,suitTemplates);
            // The suits ♠/♣ are visually similar; require separation from second choice.
            if(rank.score<0.84||rank.margin<0.035||suit.score<0.89||suit.margin<0.018)
                return new Result(cards,false);
            String card=rank.label+suit.label;
            if(cards.contains(card))return new Result(cards,false);
            cards.add(card);
        }
        return new Result(cards,cards.size()==13);
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
