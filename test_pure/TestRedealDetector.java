package vn.chatheotracker;
public final class TestRedealDetector {
    static void ok(boolean v,String m){if(!v)throw new AssertionError(m);}
    public static void main(String[] args){
        RedealDetector d=new RedealDetector();
        for(int i=0;i<50;i++)ok(!d.observe(new int[]{13,13,13},true),
            "False board evidence must NEVER arm a reset at 13/13/13");
        d.observe(new int[]{12,13,13},true);
        for(int i=0;i<50;i++)ok(!d.observe(new int[]{13,13,13},true),
            "A single unreadable counter frame must not arm reset");
        for(int i=0;i<3;i++)ok(!d.observe(new int[]{12,13,13},false),"Observed play");
        for(int i=0;i<6;i++)ok(!d.observe(new int[]{13,13,13},false),"Need seven frames");
        ok(d.awaitingConfirmation(),"Pending deal visible");
        ok(d.observe(new int[]{13,13,13},false),"Fresh stable deal");
        for(int i=0;i<200;i++)ok(!d.observe(new int[]{13,13,13},true),
            "Prevent logged 14-repeat reset storm with public board fragments");
        // Missing badges briefly interrupt the 7-frame confirmation.
        for(int i=0;i<3;i++)d.observe(new int[]{9,-1,13},false);
        for(int i=0;i<5;i++)ok(!d.observe(new int[]{13,13,13},false),"Still pending");
        d.observe(new int[]{13,-1,13},false);
        for(int i=0;i<6;i++)ok(!d.observe(new int[]{13,13,13},false),"Reset streak");
        ok(d.observe(new int[]{13,13,13},false),"After occlusion");
        System.out.println("TestRedealDetector OK");
    }
}
