package vn.chatheotracker;
import java.util.*;
public final class TestAdviceReentryDetector {
    private static void ok(boolean b,String m){if(!b)throw new AssertionError(m);}
    public static void main(String[] args){
        AdviceReentryDetector d=new AdviceReentryDetector();
        long now=10000;
        for(int i=0;i<20;i++)ok(!d.observe(true,true,Collections.emptyList(),now+i*250),"startup must not fire");
        for(int i=0;i<3;i++)ok(!d.observe(false,true,Arrays.asList("AH"),16000+i*250),"occupied not trigger");
        // Normal turn flag may never go false if action remains visually green.
        for(int i=0;i<3;i++)ok(!d.observe(true,true,Collections.emptyList(),17000+i*250),"wait for stability");
        ok(d.observe(true,true,Collections.emptyList(),17800),"cleared trick re-triggers");
        for(int i=0;i<12;i++)ok(!d.observe(true,true,Collections.emptyList(),18000+i*250),"one suggestion only");
        for(int i=0;i<3;i++)d.observe(true,true,Arrays.asList("7C"),22000+i*250);
        ok(!d.observe(true,false,Collections.emptyList(),22800),"occluded table not trigger");
        for(int i=0;i<3;i++)ok(!d.observe(false,true,Collections.emptyList(),23000+i*250),"not our turn");
        ok(d.observe(true,true,Collections.emptyList(),23900),"trigger once action returns");
        d.reset();
        ok(!d.observe(true,true,Collections.emptyList(),26000),"reset clears history");
        System.out.println("TestAdviceReentryDetector OK");
    }
}
