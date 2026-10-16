package vn.chatheotracker;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import java.io.IOException;
import java.io.InputStream;
import android.graphics.BitmapFactory;
import java.util.*;

/** Video-adapted recognizer for the center of the 2048x942 virtual canvas.
 * Samples were extracted from user-supplied screenshots/video; not an independently validated model. */
final class CardRecognizer {
    private static final int REF_W=2048, REF_H=942;
    private final Map<String,boolean[][]> rankTemplates = new LinkedHashMap<>();
    private final Map<String,boolean[][]> suitTemplates = new LinkedHashMap<>();
    CardRecognizer(Context ctx) {
        for(String r:new String[]{"3","4","5","6","7","8","9","10","J","Q","K","A","2"})
            load(ctx,"rank_"+r+".png",r,rankTemplates);
        for(String s:new String[]{"S","H","D","C"})
            load(ctx,"suit_"+s+".png",s,suitTemplates);
    }
    private void load(Context c,String file,String key,Map<String,boolean[][]> into){
        try(InputStream s=c.getAssets().open(file)) {
            Bitmap b=BitmapFactory.decodeStream(s);into.put(key,toMask(b));b.recycle();
        }catch(IOException ignored){}
    }
    private static boolean[][] toMask(Bitmap b) {
        boolean[][] m=new boolean[b.getHeight()][b.getWidth()];
        for(int y=0;y<b.getHeight();y++)for(int x=0;x<b.getWidth();x++)
            m[y][x]=Color.red(b.getPixel(x,y))>=120;
        return m;
    }
    private static boolean ink(int argb) {
        int r=Color.red(argb),g=Color.green(argb),b=Color.blue(argb);
        return (r>90 && r>g*1.4 && r>b*1.4) || (r<120 && g<120 && b<120);
    }
    private static boolean white(int a) {
        return Color.red(a)>215 && Color.green(a)>215 && Color.blue(a)>215;
    }
    // Gray-backed older play: in the supplied capture, background is around RGB(185,185,185).
    private static boolean gray(int a) {
        int r=Color.red(a),g=Color.green(a),b=Color.blue(a);
        int min=Math.min(r,Math.min(g,b)),max=Math.max(r,Math.max(g,b));
        return min>=155 && max<=210 && max-min<15;
    }
    /** Result may be partial; unknown glyphs are deliberately not counted. */
    List<String> recognize(Bitmap input){
        Bitmap pic = (input.getWidth()==REF_W && input.getHeight()==REF_H) ? input:
            Bitmap.createScaledBitmap(input,REF_W,REF_H,false);
        try { return detect(pic); }
        finally { if(pic!=input)pic.recycle(); }
    }
    /** Only the active white-front layer. Gray historical cards must not be treated as the current lead. */
    List<String> recognizeCurrentPlay(Bitmap input){
        Bitmap pic=(input.getWidth()==REF_W && input.getHeight()==REF_H)?input:
            Bitmap.createScaledBitmap(input,REF_W,REF_H,false);
        try{return detectLayer(pic,false,545);}
        finally{if(pic!=input)pic.recycle();}
    }
    private List<String> detect(Bitmap b){
        // The game dims older plays to gray and presents the latest play in white.
        // Scanning white only silently loses older (still visible) cards in a stack.
        LinkedHashSet<String> cards=new LinkedHashSet<>();
        cards.addAll(detectLayer(b,true));
        cards.addAll(detectLayer(b,false));
        return new ArrayList<>(cards);
    }
    private List<String> detectLayer(Bitmap b,boolean shaded){return detectLayer(b,shaded,650);}
    private List<String> detectLayer(Bitmap b,boolean shaded,int bottomLimit){
        int left=650,right=1400,top=shaded?295:300,bottom=shaded?420:Math.min(650,bottomLimit);
        int y0=-1;
        for(int y=top;y<bottom;y+=2){
            int count=0;
            for(int x=left;x<right;x+=2)
                if(shaded?gray(b.getPixel(x,y)):white(b.getPixel(x,y)))count++;
            if(count>35){y0=y;break;}
        }
        if(y0<0)return Collections.emptyList();
        int x0=-1,x1=-1;
        for(int x=left;x<right;x++){
            int count=0;
            for(int y=y0+6;y<y0+64;y+=2)
                if(shaded?gray(b.getPixel(x,y)):white(b.getPixel(x,y)))count++;
            if(count>=9){if(x0<0)x0=x;x1=x;}
        }
        if(x0<0||x1-x0<65||x1-x0>630)return Collections.emptyList();
        ArrayList<Blob> glyphs=components(b,x0+1,y0+2,x1,Math.min(y0+57,650));
        glyphs.removeIf(z->z.area<65 || z.h<19 || z.w<5 || z.w>70);
        glyphs.sort(Comparator.comparingInt(z->z.x));
        // A "10" is split into connected components for the 1 and the 0.
        // Some 1 glyphs in the video are 14 px, so <12 missed them.
        for(int i=0;i<glyphs.size()-1;i++){
            Blob z=glyphs.get(i),n=glyphs.get(i+1);
            if(z.w<=17 && n.x-(z.x+z.w)<13 && Math.abs(z.y-n.y)<12){
                glyphs.set(i,Blob.join(z,n));glyphs.remove(i+1);
            }
        }
        ArrayList<String> result=new ArrayList<>();
        int firstGlyph=-1;
        for(Blob z:glyphs){
            Match rank=classify(z,rankTemplates,32,40);
            // The large suit at the top-right of the final card resembles a glyph
            // but is not a *new* card. The real 10 is the only permitted wide rank.
            if(z.w>=35 && (!rank.name.equals("10") || rank.score<0.78))continue;
            if(rank.score<0.68)continue;
            if(firstGlyph>=0){
                int dx=z.x-firstGlyph,nearest=(int)Math.round(dx/46.5);
                if(nearest<1 || Math.abs(dx-nearest*46.5)>8)continue;
            }
            ArrayList<Blob> suits=components(b,Math.max(x0,z.x-8),y0+52,
                Math.min(x1,z.x+62),Math.min(650,y0+105));
            Match bestSuit=new Match("",0);
            for(Blob zSuit:suits){
                if(zSuit.area<70 || zSuit.w<13 || zSuit.h<14 || zSuit.w>50 || zSuit.h>45)continue;
                if(Math.abs((zSuit.x+zSuit.w/2)-(z.x+z.w/2))>24)continue;
                Match m=classify(zSuit,suitTemplates,28,28);
                if(m.score>bestSuit.score)bestSuit=m;
            }
            if(bestSuit.score<0.65)continue;
            if(firstGlyph<0)firstGlyph=z.x;
            String card=rank.name+bestSuit.name;
            if(!result.contains(card))result.add(card);
        }
        return result;
    }
    private static final class Blob {
        int x,y,w,h,area;boolean[][] data;
        Blob(int X,int Y,int W,int H,int A,boolean[][] D) {x=X;y=Y;w=W;h=H;area=A;data=D;}
        static Blob join(Blob a,Blob b){
            int x=Math.min(a.x,b.x),y=Math.min(a.y,b.y);
            int w=Math.max(a.x+a.w,b.x+b.w)-x,h=Math.max(a.y+a.h,b.y+b.h)-y;
            boolean[][] pixels=new boolean[h][w];
            for(Blob z:new Blob[]{a,b})for(int yy=0;yy<z.h;yy++)for(int xx=0;xx<z.w;xx++)
                if(z.data[yy][xx])pixels[z.y-y+yy][z.x-x+xx]=true;
            return new Blob(x,y,w,h,a.area+b.area,pixels);
        }
    }
    private static final class Match {
        String name;double score;Match(String a,double b){name=a;score=b;}
    }
    private Match classify(Blob z,Map<String,boolean[][]> templates,int ow,int oh){
        boolean[][] sample=resize(z.data,ow,oh);
        Match result=new Match("",0);
        for(Map.Entry<String,boolean[][]> en:templates.entrySet()){
            double score=dice(sample,en.getValue());
            if(score>result.score)result=new Match(en.getKey(),score);
        }
        return result;
    }
    private static boolean[][] resize(boolean[][] src,int w,int h){
        boolean[][] dst=new boolean[h][w];int sh=src.length,sw=src[0].length;
        for(int y=0;y<h;y++)for(int x=0;x<w;x++)
            dst[y][x]=src[Math.min(sh-1,y*sh/h)][Math.min(sw-1,x*sw/w)];
        return dst;
    }
    private static double dice(boolean[][] a,boolean[][] b){
        int same=0,one=0,two=0;
        for(int y=0;y<a.length;y++)for(int x=0;x<a[0].length;x++){
            if(a[y][x])one++;
            if(b[y][x])two++;
            if(a[y][x]&&b[y][x])same++;
        }
        return 2.0*same/Math.max(1,one+two);
    }
    private static ArrayList<Blob> components(Bitmap b,int x0,int y0,int x1,int y1){
        int w=x1-x0,h=y1-y0;
        if(w<=0||h<=0)return new ArrayList<>();
        boolean[][] m=new boolean[h][w],visited=new boolean[h][w];
        for(int y=0;y<h;y++)for(int x=0;x<w;x++)m[y][x]=ink(b.getPixel(x0+x,y0+y));
        ArrayList<Blob> all=new ArrayList<>();
        int[] queue=new int[w*h];
        for(int sy=0;sy<h;sy++)for(int sx=0;sx<w;sx++){
            if(!m[sy][sx]||visited[sy][sx])continue;
            int start=0,end=0;queue[end++]=sy*w+sx;visited[sy][sx]=true;
            int xmin=sx,xmax=sx,ymin=sy,ymax=sy;
            while(start<end){int id=queue[start++],cx=id%w,cy=id/w;
                xmin=Math.min(xmin,cx);xmax=Math.max(xmax,cx);
                ymin=Math.min(ymin,cy);ymax=Math.max(ymax,cy);
                for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++){
                    if(dx==0&&dy==0)continue;
                    int nx=cx+dx,ny=cy+dy;
                    if(nx>=0&&nx<w&&ny>=0&&ny<h&&!visited[ny][nx]&&m[ny][nx]){
                        visited[ny][nx]=true;queue[end++]=ny*w+nx;
                    }
                }
            }
            if(end<30)continue;
            int bw=xmax-xmin+1,bh=ymax-ymin+1;
            boolean[][] crop=new boolean[bh][bw];
            for(int i=0;i<end;i++){
                int id=queue[i];crop[id/w-ymin][id%w-xmin]=true;
            }
            all.add(new Blob(x0+xmin,y0+ymin,bw,bh,end,crop));
        }
        return all;
    }
}
