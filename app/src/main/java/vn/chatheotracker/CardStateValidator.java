package vn.chatheotracker;
import java.util.*;
/** Pure-Java 52-card integrity check shared by HUD, rescanning and suggestions. */
final class CardStateValidator {
    static boolean complete(Collection<String> myHand,Collection<String> played,int[] opponents){
        if(myHand==null||myHand.isEmpty()||played==null||opponents==null||opponents.length!=3)return false;
        HashSet<String> hand=new HashSet<>(myHand);
        HashSet<String> spent=new HashSet<>(played);
        if(hand.size()!=myHand.size()||spent.size()!=played.size())return false;
        for(String card:hand)if(spent.contains(card))return false;
        int total=hand.size()+spent.size();
        for(int n:opponents){if(n<0||n>13)return false;total+=n;}
        return total==52;
    }
    private CardStateValidator(){}
}
