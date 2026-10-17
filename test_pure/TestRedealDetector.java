package vn.chatheotracker;
public final class TestRedealDetector {
    static void ok(boolean v,String m){if(!v)throw new AssertionError(m);}
    public static void main(String[] args){
        RedealDetector detector=new RedealDetector();
        for(int i=0;i<20;i++)ok(!detector.observe(new int[]{13,13,13},false),"Do not reset at game start");
        ok(!detector.observe(new int[]{12,13,13},true),"Playing does not directly reset");
        for(int i=0;i<6;i++)ok(!detector.observe(new int[]{13,13,13},false),"Need stable 7 frames");
        ok(detector.observe(new int[]{13,13,13},false),"Fresh 13/13/13 is redeal");
        for(int i=0;i<15;i++)ok(!detector.observe(new int[]{13,13,13},false),"No repeated reset");
        // One missed OCR frame should restart stability count.
        detector.observe(new int[]{7,6,8},true);
        for(int i=0;i<5;i++)ok(!detector.observe(new int[]{13,13,13},false),"Wait for stable 13");
        ok(!detector.observe(new int[]{13,-1,13},false),"Badge occlusion must interrupt streak");
        for(int i=0;i<6;i++)ok(!detector.observe(new int[]{13,13,13},false),"Streak resets after occlusion");
        ok(detector.observe(new int[]{13,13,13},false),"Reset on stable return");
        System.out.println("TestRedealDetector OK");
    }
}
