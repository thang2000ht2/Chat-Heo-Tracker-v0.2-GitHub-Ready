package vn.chatheotracker;
public final class TestHandEndVisibility {
 static void ok(boolean x,String s){if(!x)throw new AssertionError(s);}
 public static void main(String[] a){
  HandEndVisibility g=new HandEndVisibility();
  for(int i=0;i<30;i++)ok(!g.observe(false,new int[]{13,13,13}),"Missing fan at start is not game over");
  for(int i=0;i<7;i++)ok(!g.observe(false,new int[]{5,0,5}),"Transient occlusion");
  ok(g.observe(false,new int[]{5,0,5}),"Result-like missing fan should hide stale J spade");
  ok(!g.observe(true,new int[]{5,0,5}),"Hand returned after occlusion");
  for(int i=0;i<8;i++)g.observe(false,new int[]{5,0,5});
  g.clear();ok(!g.observe(false,new int[]{5,0,5}),"Reset clears suspect-result status");
  System.out.println("TestHandEndVisibility OK");
 }
}
