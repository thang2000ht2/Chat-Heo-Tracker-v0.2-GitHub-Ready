package vn.chatheotracker;
import android.graphics.*;
import android.content.Context;
import javax.imageio.ImageIO;
import java.io.*;
public class TestActualBoardImages {
  public static void main(String[] argv)throws Exception{
   CardRecognizer r=new CardRecognizer(new Context());
   for(int i=1;i<=3;i++){
    Bitmap b=new Bitmap(ImageIO.read(new File("test_images/bug_board_"+i+".png")));
    java.util.List<String> all=r.recognize(b), current=r.recognizeCurrentPlay(b);
    System.out.println("board_"+i+" all="+all+" current="+current);
    if(i==3&&!current.equals(java.util.Arrays.asList("8S","9C","10D","JS","QH")))
        throw new AssertionError("Must read entire five-card straight on screenshot 3");
    if(i==1&&!current.contains("9S"))throw new AssertionError("Missing 9S on screenshot 1");
   }
  }
}
