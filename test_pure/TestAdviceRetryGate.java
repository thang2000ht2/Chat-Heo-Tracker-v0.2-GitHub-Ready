package vn.chatheotracker;
public final class TestAdviceRetryGate {
    static void ok(boolean yes,String m){if(!yes)throw new AssertionError(m);}
    public static void main(String[] args){
        AdviceRetryGate r=new AdviceRetryGate();
        r.requested("hand:9 board:stale",10000);r.finished(true);
        ok(!r.shouldRetry("hand:9 board:stale",true,13000),"no same-input retry");
        ok(!r.shouldRetry("hand:10 board:ready",false,14000),"not ready");
        ok(r.shouldRetry("hand:10 board:ready",true,14000),"new evidence should retry");
        r.requested("hand:10 board:ready",14000);r.finished(false);
        ok(!r.shouldRetry("hand:11 board:ready",true,17000),"successful result no retry");
        r.requested("hand:11 board:stale",18000);r.finished(true);
        ok(!r.shouldRetry("hand:11 board:new",true,18500),"cooldown");
        r.reset();ok(!r.shouldRetry("any",true,20000),"reset");
        System.out.println("TestAdviceRetryGate OK");
    }
}
