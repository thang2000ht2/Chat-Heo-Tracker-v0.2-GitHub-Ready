package vn.chatheotracker;

import android.graphics.Bitmap;
import android.graphics.Color;

/** Detects the actual bright-green ĐÁNH button in the user's landscape capture.
 * This is a visual heuristic tied to Pig Hunters' supplied UI, not a game API. */
final class LocalTurnDetector {
    private LocalTurnDetector() {}
    static boolean isMyTurn(Bitmap frame){
        if(frame.getWidth()!=2048 || frame.getHeight()!=942)return false;
        int green=0,total=0;
        // The green action button: x≈1090–1410, y≈570–675 on normalized 2048×942.
        for(int y=582;y<=660;y+=12)for(int x=1104;x<=1392;x+=12){
            int c=frame.getPixel(x,y);
            int r=Color.red(c),g=Color.green(c),b=Color.blue(c);
            if(g>=130 && g>r*1.55 && g>b*1.45 && r<155 && b<120)green++;
            total++;
        }
        return green>=Math.max(42,total/4);
    }
}
