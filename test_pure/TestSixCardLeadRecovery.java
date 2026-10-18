package vn.chatheotracker;
import java.util.*;
public final class TestSixCardLeadRecovery {
    static void ok(boolean v,String msg){if(!v)throw new AssertionError(msg);}
    public static void main(String[] args){
        List<String> pairs=Arrays.asList("8S","8H","9D","9C","10S","10H");
        List<SixCardLeadRecovery.Drop> drops=Collections.singletonList(new SixCardLeadRecovery.Drop(1,6,2000));
        SixCardLeadRecovery.Result m=SixCardLeadRecovery.match(pairs,drops,
            Collections.emptySet(),Collections.emptySet(),3500);
        ok(m!=null&&m.seat==1&&m.cards.size()==6,"8-9-10 three pair run recovered");
        ok(SixCardLeadRecovery.match(pairs.subList(0,5),drops,Collections.emptySet(),Collections.emptySet(),3500)==null,"no missing-card invention");
        ok(SixCardLeadRecovery.match(pairs,drops,Arrays.asList("8S"),Collections.emptySet(),3500)==null,"reject already played card");
        ok(SixCardLeadRecovery.match(pairs,drops,Collections.emptySet(),Arrays.asList("10H"),3500)==null,"reject my-hand overlap");
        ok(SixCardLeadRecovery.match(pairs,Collections.singletonList(new SixCardLeadRecovery.Drop(1,5,2000)),Collections.emptySet(),Collections.emptySet(),3500)==null,"only six-card counter drop");
        ok(SixCardLeadRecovery.match(pairs,Arrays.asList(new SixCardLeadRecovery.Drop(1,6,2000),new SixCardLeadRecovery.Drop(2,6,2500)),Collections.emptySet(),Collections.emptySet(),3500)==null,"ambiguous actor must not learn");
        ok(SixCardLeadRecovery.match(pairs,drops,Collections.emptySet(),Collections.emptySet(),9000)==null,"do not match stale drop");
        ok(!SixCardLeadRecovery.legalSix(Arrays.asList("8S","8H","9D","9C","JS","JH")),"nonconsecutive pairs invalid");
        ok(SixCardLeadRecovery.legalSix(Arrays.asList("7S","8C","9D","10H","JS","QC")),"six-card straight also eligible");
        ok(!SixCardLeadRecovery.legalSix(Arrays.asList("8S","8H","9D","9C","2S","2H")),"two cannot make three pair run");
        System.out.println("TestSixCardLeadRecovery OK");
    }
}
