package vn.chatheotracker;
public class TestHandFanGeometry {
    private static void ok(boolean yes){if(!yes)throw new AssertionError("Hand geometry");}
    public static void main(String[] args){
        for(int n=1;n<=13;n++){
            int len=(int)Math.round(160+(n-1)*94.6);
            boolean[] line=new boolean[1600];
            for(int i=90;i<90+len;i++)line[i]=true;
            HandFanGeometry.Fan found=HandFanGeometry.inRow(line,300,715);
            ok(found!=null && found.count==n && found.left==390);
        }
        boolean[] noise=new boolean[1600];
        for(int i=100;i<180;i++)noise[i]=true;
        ok(HandFanGeometry.inRow(noise,300,715)==null);
        System.out.println("TestHandFanGeometry OK");
    }
}
