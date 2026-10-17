package vn.chatheotracker;
import java.util.*;
public final class TestHeoContextAndFallback {
    static void ok(boolean x,String why){if(!x)throw new AssertionError(why);}
    static List<String> cards(String s){return Arrays.asList(s.split(" +"));}
    public static void main(String[] args){
        ok(HeoWarningAdvisor.warningForMove(cards("2H"),cards("9C 9D"),.92).isEmpty(),
            "No heo warning for a pair lead and a single 2");
        ok(HeoWarningAdvisor.warningForMove(cards("10S"),Collections.emptyList(),.66).isEmpty(),
            "Holding a heo is irrelevant if suggesting ten");
        ok(HeoWarningAdvisor.warningForMove(cards("2H"),Collections.emptyList(),.14).contains("14%"),
            "Risk shown for a proposed lone heo");
        ok(HeoWarningAdvisor.warningForMove(cards("2H"),Collections.emptyList(),0).isEmpty(),
            "No phantom zero risk warning");
        List<String> my=cards("2H 3S 5S 7H 9C");
        MonteCarloAdvisor.Advice pair=MonteCarloAdvisor.adviseWithIncompleteHistory(my,
            cards("JS JC"),Collections.emptyList(),StrategyMode.PLACEMENT);
        ok(pair.title.equals("BỎ LƯỢT"),"Never suggest single two to beat a pair: "+pair.title);
        MonteCarloAdvisor.Advice realistic=MonteCarloAdvisor.adviseBehaviorAware(
            cards("2H 10S 10C"),cards("9S 9C"),cards("9S 9C"),
            new int[]{13,13,13},null,StrategyMode.PLACEMENT);
        ok(realistic.title.contains("10"),"Recommend legal pair instead of lone heo: "+realistic.title);
        ok(!realistic.detail.contains("⚠ HEO"),"No phantom heo warning while answering a pair");
        MonteCarloAdvisor.Advice unclear=MonteCarloAdvisor.adviseWithIncompleteHistory(my,
            Collections.emptyList(),Collections.emptyList(),StrategyMode.PLACEMENT);
        ok(unclear.title.startsWith("ĐÁNH "),"Fallback needs a practical recommendation");
        ok(unclear.detail.contains("chưa đủ dữ liệu") && !unclear.detail.contains("%"),
            "No fabricated risk percentage when counters are unknown");
        MonteCarloAdvisor.Advice overlapping=MonteCarloAdvisor.adviseWithIncompleteHistory(my,
            Collections.emptyList(),cards("2H"),StrategyMode.PLACEMENT);
        ok(overlapping.title.startsWith("QUÉT LẠI"),"Reject hand/played duplicate");
        ok(ScanStatus.compact("13 lá · chưa đối soát").equals("13 lá · lệch"),"Badge shorter");
        ok(ScanStatus.compact("13 lá · thiếu lịch sử 4").equals("13 lá · thiếu 4"),"Keep missing count");
        System.out.println("TestHeoContextAndFallback OK");
    }
}
