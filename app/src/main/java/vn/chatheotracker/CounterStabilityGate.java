package vn.chatheotracker;

/** Require more repeated video frames for terminal/large counter jumps. */
final class CounterStabilityGate {
    static int requiredFrames(int previous,int next){
        // One or two transient OCR frames reading 0 may mean an effect or
        // partly obscured digit, not a finished opponent.
        if(next==0)return 4;
        if(previous>=0 && next>=0 && previous-next>=6)return 4;
        if(previous>=0 && next>=0 && previous-next>=3)return 3;
        return 2;
    }
    private CounterStabilityGate(){}
}
