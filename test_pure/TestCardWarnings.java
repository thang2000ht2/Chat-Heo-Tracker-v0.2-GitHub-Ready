package vn.chatheotracker;
import java.util.*;
public final class TestCardWarnings {
 static void chk(boolean yes, String what){if(!yes)throw new AssertionError(what);}
 static List<String> cards(String... v){return Arrays.asList(v);}
 public static void main(String[] a){
  CardWarnings.Result none=CardWarnings.analyze(Collections.emptyList());
  chk(!none.hasTriple&&!none.hasQuad,"empty pool");
  CardWarnings.Result quad=CardWarnings.analyze(cards("3S","3D","4S","4C","5H","5D","6S","6C"));
  chk(quad.hasTriple&&quad.hasQuad,"4 consecutive pairs should trigger 3/4");
  for(int i=0;i<4;i++)chk(quad.inQuad[i],"4 pairs rank coverage");
  CardWarnings.Result triple=CardWarnings.analyze(cards("8S","8D","9S","9C","10H","10D"));
  chk(triple.hasTriple&&!triple.hasQuad,"only 3 pairs");
  chk(triple.inTriple[5]&&triple.inTriple[6]&&triple.inTriple[7],"triple ranks");
  CardWarnings.Result twoExcluded=CardWarnings.analyze(cards("KS","KD","AS","AD","2S","2D"));
  chk(!twoExcluded.hasTriple,"2 cannot continue A");
  CardWarnings.Result cut=CardWarnings.analyze(cards("3S","3D","4S","4C","5H"));
  chk(!cut.hasTriple,"a singleton breaks candidate");
  CardWarnings.Result full=CardWarnings.analyze(fullDeck());
  chk(full.hasQuad&&full.hasTriple,"full deck");
  chk(full.remainingByRank[12]==4,"all twos present");
  System.out.println("CardWarnings: 6 tests passed");
 }
 static Collection<String> fullDeck(){List<String> out=new ArrayList<>();for(String r:CardWarnings.RANKS)for(String s: new String[]{"S","C","D","H"})out.add(r+s);return out;}
}
