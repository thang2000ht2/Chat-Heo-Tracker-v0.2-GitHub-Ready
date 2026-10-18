package vn.chatheotracker;
import android.graphics.*;
import android.content.Context;
import javax.imageio.ImageIO;
import java.io.File;
import java.util.*;
/** JVM runs the actual Android HandRecognizer against anonymized provided screen crops. */
public class TestActualHandImages {
    static void check(String file,String... expected) throws Exception {
        HandRecognizer r=new HandRecognizer(new Context());
        Bitmap bitmap=new Bitmap(ImageIO.read(new File("test_images/"+file+".png")));
        HandRecognizer.Result result=r.recognizeVisibleHand(bitmap);
        System.out.println(file+" => "+result.cards+" ; "+result.diagnosis+" ; slots="+result.fanCount);
        if(!result.complete || !result.cards.equals(Arrays.asList(expected)))
            throw new AssertionError("Wrong hand: "+file);
    }
    public static void main(String[] args) throws Exception {
        check("selected_five","4D","9H","10H","JC","KH");
        check("spread_ten","4S","7D","7H","10H","JD","QS","KS","KD","KH","AD");
        System.out.println("TestActualHandImages OK");
    }
}
