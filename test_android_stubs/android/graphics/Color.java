package android.graphics;
public class Color {
    public static int red(int v){return (v>>16)&255;}
    public static int green(int v){return (v>>8)&255;}
    public static int blue(int v){return v&255;}
}
