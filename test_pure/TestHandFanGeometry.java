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
        java.util.List<Integer> raised=java.util.Arrays.asList(
            844,844,938,938,1032,1032,1127,1127,1221,1221,1168);
        HandFanGeometry.Fan fan=HandFanGeometry.fromWhiteEdges(raised,714);
        ok(fan!=null && fan.count==5 && Math.abs(fan.left-844)<=3);
        ok(HandFanGeometry.fromWhiteEdges(java.util.Arrays.asList(900,901,902),714)==null);
        System.out.println("TestHandFanGeometry OK");
    }
}
