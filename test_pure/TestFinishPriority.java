package vn.chatheotracker;
public final class TestFinishPriority {
    static void ok(boolean b,String msg){if(!b)throw new AssertionError(msg);}
    public static void main(String[] args){
        double oneTurn=FinishPriority.cost(1,5,3,.65,0,8,StrategyMode.PLACEMENT);
        double threeTurns=FinishPriority.cost(3,6,2,.05,0,8,StrategyMode.PLACEMENT);
        ok(oneTurn<threeTurns,"placement should prefer a short route over mere low block risk");
        double safe=FinishPriority.cost(2,6,2,.14,0,6,StrategyMode.PLACEMENT);
        double chopped=FinishPriority.cost(2,6,2,.14,.85,6,StrategyMode.PLACEMENT);
        ok(safe<chopped,"avoid easily chopped pig at otherwise equal pace");
        double normal=FinishPriority.cost(3,8,1,.15,0,7,StrategyMode.PLACEMENT);
        double desperate=FinishPriority.cost(3,8,1,.15,0,1,StrategyMode.PLACEMENT);
        ok(desperate>normal,"unfinished cards more costly when opponent about to finish");
        System.out.println("TestFinishPriority OK");
    }
}
