package android.util;
public class Log {
    public static int w(String tag,String m,Throwable e){System.err.println(tag+": "+m+" " + e);return 0;}
}
