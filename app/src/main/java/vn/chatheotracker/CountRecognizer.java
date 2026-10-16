package vn.chatheotracker;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Reads the visible card-back counters. P1 = top, P2 = left, P3 = right.
 * Coordinates are normalized to the same 2048 x 942 game layout as CardRecognizer.
 * White digit templates for 0,1,2,3,4,5,7,9 were extracted from the user's video.
 * Templates 6/8 are approximate and have not been validated against this game's glyphs.
 */
final class CountRecognizer {
    static final int REF_W=2048, REF_H=942;
    private static final int[] CX={1174,335,1744};
    private static final int[] CY={148,386,386};
    private final boolean[][][] templates=new boolean[10][][];
    CountRecognizer(Context context){
        for(int d=0;d<10;d++){
            try(InputStream in=context.getAssets().open("count_digit_"+d+".png")){
                Bitmap bitmap=BitmapFactory.decodeStream(in);
                boolean[][] pixels=new boolean[bitmap.getHeight()][bitmap.getWidth()];
                for(int y=0;y<bitmap.getHeight();y++)for(int x=0;x<bitmap.getWidth();x++)
                    pixels[y][x]=Color.red(bitmap.getPixel(x,y))>127;
                templates[d]=pixels;bitmap.recycle();
            }catch(Exception ignored){}
        }
    }
    int[] read(Bitmap frame){
        Bitmap ref=frame;
        if(frame.getWidth()!=REF_W || frame.getHeight()!=REF_H)
            ref=Bitmap.createScaledBitmap(frame,REF_W,REF_H,false);
        try {
            int[] counts=new int[3];
            for(int i=0;i<3;i++)counts[i]=readAt(ref,CX[i],CY[i]);
            return counts;
        }finally {if(ref!=frame)ref.recycle();}
    }
    private static boolean white(int c){
        int r=Color.red(c),g=Color.green(c),b=Color.blue(c);
        int low=Math.min(r,Math.min(g,b)), high=Math.max(r,Math.max(g,b));
        return low>185 && high-low<70;
    }
    private int readAt(Bitmap bmp,int x,int y){
        // If a player goes out, this game removes that player's orange card-back.
        // Return 0 only as a visual 'badge missing' signal; the service will
        // accept it only after a previously readable count of <= 3.
        int orange=0;
        for(int yy=y-50;yy<y+50;yy+=2)for(int xx=x-38;xx<x+38;xx+=2){
            int c=bmp.getPixel(xx,yy);
            int r=Color.red(c),g=Color.green(c),b=Color.blue(c);
            if(r>135 && g>60 && g<210 && b<110 && r>g*1.15)orange++;
        }
        if(orange<175)return 0;
        final int width=59,height=33;
        boolean[][] mask=new boolean[height][width],seen=new boolean[height][width];
        for(int j=0;j<height;j++)for(int i=0;i<width;i++)
            mask[j][i]=white(bmp.getPixel(x-29+i,y-16+j));
        ArrayList<Glyph> glyphs=new ArrayList<>();
        int[] q=new int[width*height];
        for(int sy=0;sy<height;sy++)for(int sx=0;sx<width;sx++){
            if(!mask[sy][sx]||seen[sy][sx])continue;
            int start=0,end=0;seen[sy][sx]=true;q[end++]=sy*width+sx;
            int x1=sx,x2=sx,y1=sy,y2=sy;
            while(start<end){
                int index=q[start++];int xx=index%width,yy=index/width;
                x1=Math.min(x1,xx);x2=Math.max(x2,xx);
                y1=Math.min(y1,yy);y2=Math.max(y2,yy);
                for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++){
                    int nx=xx+dx,ny=yy+dy;
                    if(nx>=0&&nx<width&&ny>=0&&ny<height&&mask[ny][nx]&&!seen[ny][nx]){
                        seen[ny][nx]=true;q[end++]=ny*width+nx;
                    }
                }
            }
            int gw=x2-x1+1,gh=y2-y1+1;
            if(gw<6||gw>24||gh<17||gh>27||end<60)continue;
            boolean[][] part=new boolean[gh][gw];
            for(int j=0;j<gh;j++)for(int i=0;i<gw;i++)part[j][i]=mask[y1+j][x1+i];
            glyphs.add(new Glyph(x1,part));
        }
        if(glyphs.size()<1||glyphs.size()>2)return -1;
        glyphs.sort((a,b)->Integer.compare(a.x,b.x));
        int number=0;
        for(Glyph glyph:glyphs){
            int bestDigit=-1;double bestScore=0;
            for(int d=0;d<10;d++){
                if(templates[d]==null)continue;
                double score=score(glyph.pixels,templates[d]);
                if(score>bestScore){bestScore=score;bestDigit=d;}
            }
            // Deliberately leave uncertain counters unread instead of inventing a turn.
            if(bestDigit<0||bestScore<0.65)return -1;
            number=number*10+bestDigit;
        }
        return number>=1 && number<=13 ? number : -1;
    }
    private static double score(boolean[][] shape,boolean[][] template){
        final int w=20,h=26;
        boolean[][] a=resize(shape,w,h),b=resize(template,w,h);
        int one=0,two=0,both=0;
        for(int y=0;y<h;y++)for(int x=0;x<w;x++){
            if(a[y][x])one++;
            if(b[y][x])two++;
            if(a[y][x]&&b[y][x])both++;
        }
        double ratioA=(double)shape[0].length/shape.length;
        double ratioB=(double)template[0].length/template.length;
        return 2.0*both/Math.max(1,one+two)-0.17*Math.abs(ratioA-ratioB);
    }
    private static boolean[][] resize(boolean[][] src,int w,int h){
        boolean[][] out=new boolean[h][w];
        int H=src.length,W=src[0].length;
        for(int y=0;y<h;y++)for(int x=0;x<w;x++)
            out[y][x]=src[Math.min(H-1,y*H/h)][Math.min(W-1,x*W/w)];
        return out;
    }
    private static class Glyph {
        final int x;final boolean[][] pixels;
        Glyph(int xx,boolean[][] data){x=xx;pixels=data;}
    }
}
