package vn.chatheotracker;
public class TestCounterStabilityGate {
    static void ok(boolean v,String m){if(!v)throw new AssertionError(m);}
    public static void main(String[] args){
        ok(CounterStabilityGate.requiredFrames(2,0)==4,"terminal zero requires four captures");
        ok(CounterStabilityGate.requiredFrames(3,0)==4,"three to zero also terminal");
        ok(CounterStabilityGate.requiredFrames(12,8)==3,"large card drops need extra confirmation");
        ok(CounterStabilityGate.requiredFrames(7,6)==2,"single card drops remain responsive");
        ok(CounterStabilityGate.requiredFrames(11,13)==2,"redeal state remains independent");
        System.out.println("TestCounterStabilityGate OK");
    }
}
