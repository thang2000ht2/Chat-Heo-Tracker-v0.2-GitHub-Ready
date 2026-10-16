package vn.chatheotracker;
import java.util.*;
public final class TestCardStateValidator {
    static void ok(boolean yes){if(!yes)throw new AssertionError("bad conservation result");}
    public static void main(String[] args){
        List<String> hand=Arrays.asList("3S","4C","2H");
        List<String> seen=new ArrayList<>();
        String[] ranks={"3","4","5","6","7","8","9","10","J","Q","K","A","2"};
        for(String rank:ranks)for(String suit:new String[]{"S","C","D","H"}){
            String c=rank+suit;
            if(!hand.contains(c))seen.add(c);
        }
        // 49 played, 3 remaining in hand, 0 for other players.
        ok(CardStateValidator.complete(hand,seen,new int[]{0,0,0}));
        ok(!CardStateValidator.complete(hand,seen,new int[]{1,0,0}));
        ok(!CardStateValidator.complete(hand,seen.subList(1,seen.size()),new int[]{0,0,0}));
        ok(!CardStateValidator.complete(hand,Arrays.asList("3S"),new int[]{13,13,13}));
        ok(!CardStateValidator.complete(Arrays.asList("3S","3S"),seen,new int[]{0,0,0}));
        System.out.println("TestCardStateValidator OK");
    }
}
