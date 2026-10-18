package vn.chatheotracker;
public final class TestCaptureReadinessGate {
    static void ok(boolean x,String m){if(!x)throw new AssertionError(m);}
    public static void main(String[] a){
        CaptureReadinessGate g=new CaptureReadinessGate(0);
        for(int i=0;i<12;i++)ok(g.observe(false,1000+i*600)==CaptureReadinessGate.Status.WAITING,"early empty frames remain waiting");
        ok(g.observe(false,12010)==CaptureReadinessGate.Status.REBIND_ONCE,"auto recover once when game absent");
        ok(g.observe(false,15000)==CaptureReadinessGate.Status.WAITING,"do not rebind repeatedly");
        ok(g.observe(false,22500)==CaptureReadinessGate.Status.NEED_GAME_OR_PERMISSION,"explicit actionable status");
        ok(g.observe(true,23000)==CaptureReadinessGate.Status.GAME_VISIBLE,"game appears without restart");
        ok(g.observe(false,24000)==CaptureReadinessGate.Status.GAME_VISIBLE,"game was observed previously");
        System.out.println("TestCaptureReadinessGate OK");
    }
}
