package vn.chatheotracker;
public final class TestCounterTransitionPolicy {
    private static void ok(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    public static void main(String[] args){
        ok(CounterTransitionPolicy.rejection(-1,8)==null,"unknown -> initial 8 is baseline");
        ok(CounterTransitionPolicy.rejection(8,6)==null,"valid decrease");
        ok(CounterTransitionPolicy.rejection(3,0)!=null,"absent badge cannot become zero");
        ok(CounterTransitionPolicy.rejection(2,-1)!=null,"unreadable badge never a move");
        ok(CounterTransitionPolicy.rejection(3,5).equals("unexpected_counter_increase"),"no increase within round");
        ok(CounterTransitionPolicy.rejection(5,13)!=null,"one seat cannot trigger a redeal");
        ok(CounterTransitionPolicy.rejection(13,1)==null,"large observable move may be valid");
        ok(!CounterTransitionPolicy.mayRecordAsMove(-1,5),"initialization not an opponent play");
        ok(!CounterTransitionPolicy.mayRecordAsMove(5,13),"redeal not play");
        ok(CounterTransitionPolicy.mayRecordAsMove(8,1),"7 card move supported");
        System.out.println("TestCounterTransitionPolicy OK");
    }
}
