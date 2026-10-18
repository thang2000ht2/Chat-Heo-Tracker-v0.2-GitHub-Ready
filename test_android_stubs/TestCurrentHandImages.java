package vn.chatheotracker;
import android.graphics.*;import android.content.Context;import javax.imageio.ImageIO;import java.io.File;
public class TestCurrentHandImages{
 public static void main(String[] a)throws Exception {
   HandRecognizer r=new HandRecognizer(new Context());
   for(int i=1;i<=3;i++){
    Bitmap b=new Bitmap(ImageIO.read(new File("test_images/bug_hand_"+i+".png")));
    HandRecognizer.Result v=r.recognizeVisibleHand(b);
    System.out.println("hand_"+i+" -> "+v.cards+" fan="+v.fanCount+" complete="+v.complete+" "+v.diagnosis+" slots="+v.slots);
    java.util.List<String> expected=i==1?
        java.util.Arrays.asList("JD","JH","KS"):
        java.util.Arrays.asList("4H","7C","8D","9H","10C","QC","KC","AD");
    if(!v.complete||!v.cards.equals(expected))throw new AssertionError("Incorrect local hand in user screenshot "+i);
   }
 }
}
