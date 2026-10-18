package vn.chatheotracker;
import java.util.*;
public class TestMoveHypothesisAnalyzer {
    static void ok(boolean v,String x){if(!v)throw new AssertionError(x);}
    static boolean contains(List<MoveHypothesisAnalyzer.Hypothesis> h,String shape,String ranks){
        for(MoveHypothesisAnalyzer.Hypothesis c:h)if(shape.equals(c.shape)&&ranks.equals(c.ranks))return true;
        return false;
    }
    public static void main(String[] args){
        List<String> partial=Arrays.asList("8S","8H","9C","9D","10H","10C");
        List<MoveHypothesisAnalyzer.Hypothesis> possibilities=
            MoveHypothesisAnalyzer.possible(8,partial,Collections.emptyList());
        ok(contains(possibilities,"FOUR_PAIRS","8-J"),"can infer possible 8-9-10-J run");
        ok(contains(possibilities,"FOUR_PAIRS","7-10"),"can infer possible 7-8-9-10 run");
        ok(!contains(possibilities,"STRAIGHT","8-A"),"not an eight-card straight because doubles visible");
        ok(!contains(MoveHypothesisAnalyzer.possible(8,partial,Arrays.asList("JS","JH","JD")),
            "FOUR_PAIRS","8-J"),"known local J cards exclude candidate without revealing unknown suits");
        ok(MoveHypothesisAnalyzer.possible(8,partial,Arrays.asList("8S")).isEmpty(),"contradictory observation rejected");
        ok(contains(MoveHypothesisAnalyzer.possible(4,Arrays.asList("9S","9C","9D"),Collections.emptySet()),
            "FOUR_KIND","9"),"quad hypothesis when fourth hidden");
        ok(!contains(MoveHypothesisAnalyzer.possible(4,Arrays.asList("9S","9C","9D"),Arrays.asList("9H")),
            "FOUR_KIND","9"),"quad excluded when fourth card already played");
        ok(MoveHypothesisAnalyzer.possible(6,Arrays.asList("8S","8H","9C","9D","10H","10C"),Collections.emptySet()).size()>0,"all observed six feasible");
        System.out.println("TestMoveHypothesisAnalyzer OK");
    }
}
