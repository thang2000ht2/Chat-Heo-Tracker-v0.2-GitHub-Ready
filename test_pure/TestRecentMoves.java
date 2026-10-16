package vn.chatheotracker;
import java.util.*;
public final class TestRecentMoves {
  private static void eq(String expected,String got){if(!expected.equals(got))throw new AssertionError("want:"+expected+" got:"+got);}
  public static void main(String[] args){
    RecentMoves h=new RecentMoves();
    h.record(Arrays.asList("3S","4H","5D"));
    h.record(Arrays.asList("8H","8S","8C"));
    h.record(Arrays.asList("KH","KC","KS"));
    eq("3♠ 4♥ 5♦  ›  8♥♠♣  ›  K♥♣♠",h.fullText());
    eq("K♥♣♠",h.mostRecent());
    eq("3♠ 4♥ 5♦  ›  8♥♠♣",h.olderMoves());
    h.remove("KC"); eq("3♠ 4♥ 5♦  ›  8♥♠♣  ›  K♥♠",h.fullText());
    h.remove("KH");h.remove("KS");eq("3♠ 4♥ 5♦  ›  8♥♠♣",h.fullText());
    h.clear();eq("",h.fullText());
    System.out.println("Chronological history tests OK");
  }
}
