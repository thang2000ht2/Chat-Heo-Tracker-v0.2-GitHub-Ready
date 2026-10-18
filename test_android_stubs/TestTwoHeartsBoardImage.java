package vn.chatheotracker;
import android.graphics.Bitmap;
import android.content.Context;
import javax.imageio.ImageIO;
import java.io.File;
import java.util.*;
/** Screenshot 20:46:37, with only the center board retained (no identities). */
public class TestTwoHeartsBoardImage {
    public static void main(String[] args)throws Exception{
        CardRecognizer r=new CardRecognizer(new Context());
        Bitmap b=new Bitmap(ImageIO.read(new File("test_images/real_2h_active_jd_gray.png")));
        List<String> all=r.recognize(b),active=r.recognizeCurrentPlay(b);
        System.out.println("20:46:37 all="+all+" active="+active);
        if(!active.contains("2H"))throw new AssertionError("Active 2H must be found in white layer");
        if(active.contains("JD"))throw new AssertionError("Old gray JD must not be an active move");
        System.out.println("TestTwoHeartsBoardImage OK");
    }
}
