package vn.chatheotracker;

import android.graphics.Bitmap;
import android.graphics.Color;

/** Game-specific 2048x942 sampling of nickname ribbons; no image is persisted. */
final class SeatVisualSignature {
    private static final int[][] RECTS = {
        {890, 229, 1130, 267},  // top seat nickname
        {85, 451, 280, 500},    // left seat nickname
        {1760, 452, 2040, 500}  // right seat nickname
    };
    static String[] read(Bitmap frame) {
        String[] out = new String[3];
        if (frame == null || frame.getWidth() < 2048 || frame.getHeight() < 942) return out;
        for (int i = 0; i < 3; i++) {
            int[] r = RECTS[i];
            StringBuilder bits = new StringBuilder(SeatIdentityTracker.BITS);
            for (int y = 0; y < 12; y++) for (int x = 0; x < 60; x++) {
                int x0=r[0]+(r[2]-r[0])*x/60, x1=r[0]+(r[2]-r[0])*(x+1)/60;
                int y0=r[1]+(r[3]-r[1])*y/12, y1=r[1]+(r[3]-r[1])*(y+1)/12;
                int light=0,total=0;
                for(int sy=y0;sy<y1;sy++)for(int sx=x0;sx<x1;sx++) {
                    int c=frame.getPixel(sx,sy);
                    int red=Color.red(c), green=Color.green(c), blue=Color.blue(c);
                    if(red>183 && green>183 && blue>183 &&
                        Math.max(red,Math.max(green,blue)) - Math.min(red,Math.min(green,blue))<58)
                        light++;
                    total++;
                }
                bits.append(light * 100 >= total * 12 ? '1':'0');
            }
            String key=bits.toString();
            out[i]=SeatIdentityTracker.valid(key)?key:null;
        }
        return out;
    }
    private SeatVisualSignature() {}
}
