package android.graphics;
import java.awt.image.BufferedImage;
public class Bitmap {
    final BufferedImage image;
    public Bitmap(BufferedImage image){this.image=image;}
    public int getWidth(){return image.getWidth();}
    public int getHeight(){return image.getHeight();}
    public int getPixel(int x,int y){return image.getRGB(x,y);}
    public static Bitmap createScaledBitmap(Bitmap src,int w,int h,boolean filtered){
        BufferedImage output=new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D graphics=output.createGraphics();graphics.drawImage(src.image,0,0,w,h,null);graphics.dispose();return new Bitmap(output);
    }
    public void recycle(){}
}
