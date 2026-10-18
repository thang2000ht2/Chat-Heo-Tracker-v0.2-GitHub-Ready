package vn.chatheotracker;
import android.content.Context;
import android.graphics.Bitmap;
import javax.imageio.ImageIO;
import java.io.File;
import java.util.*;

/** Privacy-masked screenshots of the observed failure, not synthetic cards. */
public class TestUserOctober18Images {
 public static void main(String[] args)throws Exception{
  Context ctx=new Context();
  HandRecognizer r=new HandRecognizer(ctx);
  Bitmap nine=new Bitmap(ImageIO.read(new File("test_images/real_nine_local.png")));
  HandRecognizer.Result hand=r.recognizeVisibleHand(nine);
  List<String> want=Arrays.asList("5H","9H","10S","JS","JH","KS","KH","AD","2C");
  System.out.println("real_nine_local => "+hand.cards+" complete="+hand.complete+" "+hand.diagnosis);
  if(!hand.complete || !hand.cards.equals(want))
    throw new AssertionError("Actual nine-card screenshot must be recognized: "+hand.cards);
  CardRecognizer board=new CardRecognizer(ctx);
  Bitmap end=new Bitmap(ImageIO.read(new File("test_images/real_end_straight.png")));
  List<String> observed=board.recognize(end);
  System.out.println("real_end_straight => "+observed);
  if(!new HashSet<>(observed).containsAll(Arrays.asList("3C","4D","5C","6H","7D")))
    throw new AssertionError("Real end-game five-card straight read incorrectly: "+observed);
  System.out.println("TestUserOctober18Images OK");
 }
}
