package vn.chatheotracker;
import java.util.*;

public final class TestRecentMoves {
    private static void assertEquals(String want, String got) {
        if (!want.equals(got)) throw new AssertionError("expected="+want+" actual="+got);
    }
    public static void main(String[] args) {
        RecentMoves h = new RecentMoves();
        h.record(Arrays.asList("3S","4H","5D"));
        h.record(Arrays.asList("8H","8S","8C"));
        h.record(Arrays.asList("KH","KC","KS"));
        assertEquals("K♥♣♠  ›  8♥♠♣  ›  3♠ 4♥ 5♦", h.fullText());
        assertEquals("K♥♣♠", h.mostRecent());
        h.remove("KC");
        assertEquals("K♥♠  ›  8♥♠♣  ›  3♠ 4♥ 5♦", h.fullText());
        h.remove("KS"); h.remove("KH");
        assertEquals("8♥♠♣  ›  3♠ 4♥ 5♦", h.fullText());
        h.record(Arrays.asList("2H","4D","3C"));
        assertEquals("2♥ 4♦ 3♣  ›  8♥♠♣  ›  3♠ 4♥ 5♦", h.fullText());
        h.clear(); assertEquals("", h.fullText());
        System.out.println("TestRecentMoves: 6 assertions passed");
    }
}
