package vn.chatheotracker;
import java.util.*;
public final class TestStrategyAdvisor {
  static void ok(boolean p,String msg){if(!p)throw new AssertionError(msg);}
  public static void main(String[] args){
    StrategyAdvisor.Input a=StrategyAdvisor.parse("3♠,4♥ 5♦ 7C 7S AS 2H");
    ok(a.error==null && a.cards.size()==7,"parse cards");
    ok(StrategyAdvisor.parse("6D 6♦").error!=null,"duplicate card");
    ok(StrategyAdvisor.parse("11♠").error!=null,"invalid rank");
    StrategyAdvisor.Result r=StrategyAdvisor.suggest(a.cards,Collections.emptyList(),new int[]{13,9,1});
    ok(r.title.contains("Cân nhắc ra")&&r.detail.contains("chỉ còn 1 lá"),"on-demand lead");
    StrategyAdvisor.Result block=StrategyAdvisor.suggest(Arrays.asList("6S","7D"),Collections.singletonList("5S"),new int[]{13,13,13});
    ok(block.title.contains("6♠"),"minimal beat");
    StrategyAdvisor.Result wait=StrategyAdvisor.suggest(Arrays.asList("3S","4H"),Arrays.asList("10S","10C"),null);
    ok(wait.title.contains("Chưa có nước chặn"),"don't invent legal beat");
    System.out.println("StrategyAdvisor tests OK");
  }
}
