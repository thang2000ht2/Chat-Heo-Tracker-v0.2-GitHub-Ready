package vn.chatheotracker;
import java.util.*;
public class TestTwoHeartsRegression {
    private static void ok(boolean x,String m){if(!x)throw new AssertionError(m);}
    public static void main(String[] args){
        List<String> mixed=Arrays.asList("2H","JD","2C");
        LinkedHashSet<String> active=ActiveBoardEvidence.active(mixed,Collections.singletonList("2C"));
        ok(active.size()==1&&active.contains("2C")&&!active.contains("2H"),
            "gray historical 2H must not become new played-card evidence");
        TemporalCardEvidence ev=new TemporalCardEvidence();
        ev.update(active,Collections.emptySet(),Collections.emptySet(),1000);
        LinkedHashSet<String> confirmed=ev.update(active,Collections.emptySet(),Collections.emptySet(),1250);
        ok(confirmed.equals(new LinkedHashSet<>(Collections.singletonList("2C"))),
            "only actual white lead becomes confirmed");
        List<PlayBatchMatcher.Fragment> wrong=Collections.singletonList(
            new PlayBatchMatcher.Fragment(Collections.singletonList("2H"),1300,1500));
        List<PlayBatchMatcher.Drop> drop=Collections.singletonList(new PlayBatchMatcher.Drop(0,1,1400));
        ok(PlayBatchMatcher.exact(wrong,drop,Collections.singletonList("2C"),false,1600)==null,
            "unstable board cannot authorize 2H move");
        ok(PlayBatchMatcher.exact(wrong,drop,Collections.singletonList("2C"),true,1600)==null,
            "stable 2C cannot authorize 2H move");
        List<PlayBatchMatcher.Fragment> right=Collections.singletonList(
            new PlayBatchMatcher.Fragment(Collections.singletonList("2C"),1300,1500));
        ok(PlayBatchMatcher.exact(right,drop,Collections.singletonList("2C"),true,1600)!=null,
            "stable 2C still recognized normally");
        ok(PlayBatchMatcher.exact(Collections.singletonList(
            new PlayBatchMatcher.Fragment(Collections.singletonList("2H"),5500,5700)),
            Collections.singletonList(new PlayBatchMatcher.Drop(1,1,5600)),
            Collections.singletonList("2H"),true,5850)!=null,
            "actual 2H on a later lead remains eligible");
        ok(ActiveBoardEvidence.active(Collections.singletonList("JD"),Collections.emptyList()).isEmpty(),
            "no WHITE layer means no new-card evidence");
        System.out.println("TestTwoHeartsRegression OK");
    }
}
