package vn.chatheotracker;
import java.util.*;
public final class TestLocalPlayEvidence {
 static void ok(boolean v,String m){if(!v)throw new AssertionError(m);}
 public static void main(String[] args){
  List<String> old=Arrays.asList("4C","5S","6S","7H","5H","9H","10S","JS","JH","KS","KH","AD","2C");
  List<String> now=Arrays.asList("5H","9H","10S","JS","JH","KS","KH","AD","2C");
  List<String> board=Arrays.asList("4C","5S","6S","7H");
  ok(new HashSet<>(LocalPlayEvidence.confirmed(old,now,board)).equals(new HashSet<>(board)),
   "Our four-card lead must be identified when opponents remain at 13/13/13");
  ok(LocalPlayEvidence.confirmed(old,now,Arrays.asList("4C","5S")).isEmpty(),
   "Missing board cards might be occluded: never guess them");
  ok(LocalPlayEvidence.confirmed(old,Arrays.asList("3C","9H"),board).isEmpty(),
   "Unrelated visual hand is not shrinkage");
  ok(LocalPlayEvidence.confirmed(now,now,board).isEmpty(),"Same hand no move");
  ok(new HashSet<>(LocalPlayEvidence.opening(now,board,new int[]{13,13,13}))
    .equals(new HashSet<>(board)),"Missed initial thirteen but observed our 9+4 opening");
  ok(LocalPlayEvidence.opening(now,board,new int[]{13,13,12}).isEmpty(),
    "Cannot attribute active board to us after an opponent has played");
  ok(LocalPlayEvidence.opening(Arrays.asList("9H"),board,new int[]{13,13,13}).isEmpty(),
    "Do not guess missing cards when count does not sum to thirteen");
  System.out.println("TestLocalPlayEvidence OK");
 }
}
