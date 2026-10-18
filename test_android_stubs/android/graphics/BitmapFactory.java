package android.graphics;
import java.io.*;
import javax.imageio.ImageIO;
public class BitmapFactory {
    public static Bitmap decodeStream(InputStream in) throws IOException {
        java.awt.image.BufferedImage image=ImageIO.read(in);
        return image==null?null:new Bitmap(image);
    }
}
