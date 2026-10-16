package vn.chatheotracker;
import java.util.*;
public final class TestTemporalCardEvidence {
 static void chk(boolean a,String m){if(!a)throw new AssertionError(m);}
 static Set<String> empty(){return Collections.emptySet();}
 public static void main(String[] a){
  TemporalCardEvidence ev=new TemporalCardEvidence();
  chk(ev.update(Arrays.asList("3S","4H"),empty(),empty(),1000).isEmpty(),"need two sightings");
  Set<String> a1=ev.update(Arrays.asList("3S"),empty(),empty(),1300);
  chk(a1.size()==1&&a1.contains("3S"),"occlusion must not lose other glyph");
  chk(ev.update(Arrays.asList("4H"),empty(),empty(),1550).contains("4H"),"reappear after gap");
  chk(ev.update(Arrays.asList("10C"),empty(),empty(),2000).isEmpty(),"first of 10C");
  chk(ev.update(Collections.emptyList(),empty(),empty(),2300).isEmpty(),"fully covered frame");
  chk(ev.update(Arrays.asList("10C"),empty(),empty(),2600).contains("10C"),"non-consecutive confirmation");
  chk(ev.update(Arrays.asList("AS"),empty(),empty(),3000).isEmpty(),"first before expiry");
  chk(ev.update(Arrays.asList("AS"),empty(),empty(),5000).isEmpty(),"expired must restart");
  chk(ev.update(Arrays.asList("AS"),Collections.singleton("AS"),empty(),5100).isEmpty(),"dedup played");
  ev.clear();
  chk(ev.update(Arrays.asList("QS"),empty(),empty(),6000).isEmpty(),"clear reset");
  System.out.println("TemporalCardEvidence: 10 tests passed");
 }
}
