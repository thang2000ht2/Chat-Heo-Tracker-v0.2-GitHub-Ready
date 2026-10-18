package vn.chatheotracker;
import java.util.*;
public class TestPlayBatchMatcher {
  static void ok(boolean v,String m){if(!v)throw new AssertionError(m);}
  public static void main(String[] a){
    List<PlayBatchMatcher.Fragment> f=Arrays.asList(
      new PlayBatchMatcher.Fragment(Arrays.asList("8S","9C","10D"),1000,1100),
      new PlayBatchMatcher.Fragment(Arrays.asList("JS","QH"),1600,1850));
    List<PlayBatchMatcher.Drop> d=Collections.singletonList(new PlayBatchMatcher.Drop(0,5,1100));
    ok(PlayBatchMatcher.exact(f.subList(0,1),d,Arrays.asList("8S","9C","10D","JS","QH"),true,1200)==null,"no early 3 of 5 attribution");
    PlayBatchMatcher.Match m=PlayBatchMatcher.exact(f,d,Arrays.asList("8S","9C","10D","JS","QH"),true,1950);
    ok(m!=null&&m.fragments.size()==2&&m.cards.size()==5&&m.cards.contains("QH"),"join true 5-card straight");
    ok(PlayBatchMatcher.exact(f,d,Arrays.asList("8S","9C","10D"),true,1950)!=null,"complete temporal evidence can exceed current partially visible board");
    ok(PlayBatchMatcher.exact(f,Collections.singletonList(new PlayBatchMatcher.Drop(0,3,1100)),
      Arrays.asList("8S","9C","10D","JS","QH"),true,1950)==null,"do not credit 3 of 5 when center visibly has a 5-card play");
    ok(PlayBatchMatcher.exact(f,Collections.singletonList(new PlayBatchMatcher.Drop(0,6,1100)),
      Arrays.asList("8S","9C","10D","JS","QH"),true,1950)==null,"do not invent unknown sixth card");
    System.out.println("TestPlayBatchMatcher OK");
  }
}
