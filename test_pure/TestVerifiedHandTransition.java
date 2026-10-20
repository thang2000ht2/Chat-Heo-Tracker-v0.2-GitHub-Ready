package vn.chatheotracker;
import java.util.*;
/** Regression modeled after 2026-10-20 diagnostic: 5 -> 4 -> false 1 card. */
public final class TestVerifiedHandTransition {
    private static void ok(boolean b,String text){if(!b)throw new AssertionError(text);}
    private static List<String> list(String... cards){return Arrays.asList(cards);}
    public static void main(String[] args){
        List<String> five=list("8C","8D","9S","9H","2H");
        List<String> four=list("8C","8D","9S","9H");
        List<String> one=list("9H");
        ok(VerifiedHandTransition.assess(five,four,list("2H")).confirmed(),
           "2H disappears from a verified 5-card hand AND is seen in public");
        VerifiedHandTransition.Result premature=VerifiedHandTransition.assess(
           four,one,list("2H","8C","8D"));
        ok(premature.status==VerifiedHandTransition.Status.WAIT_PUBLIC_CARDS &&
           premature.missingPublicEvidence.equals(list("9S")),
           "4 -> 1 must remain pending until 9S has been seen");
        ok(VerifiedHandTransition.assess(four,one,list("8C","8D","9S")).confirmed(),
           "A fully corroborated shrink may be accepted");
        ok(VerifiedHandTransition.assess(four,one,list("8C","8D","9S","9H")).status==
           VerifiedHandTransition.Status.REJECT_PUBLIC_OVERLAP,
           "9H visible on table cannot simultaneously be the remaining hand");
        ok(VerifiedHandTransition.assess(four,list("8C","8D","9S","JC"),list("9H")).status==
           VerifiedHandTransition.Status.REJECT_CHANGED_CARDS,
           "Cannot replace one suit with another without manual correction");
        ok(VerifiedHandTransition.assess(four,five,list("2H")).status==
           VerifiedHandTransition.Status.REJECT_CHANGED_CARDS,
           "A verified hand cannot grow during a deal");
        ok(VerifiedHandTransition.publicHandOverlap(four,list("9H","QD"))
            .equals(list("9H")),"9H on public board must flag the stale fan");
        ok(VerifiedHandTransition.assess(four,four,Collections.emptyList()).status==
           VerifiedHandTransition.Status.SAME,"Unchanged hand remains verified");
        System.out.println("TestVerifiedHandTransition OK");
    }
}
