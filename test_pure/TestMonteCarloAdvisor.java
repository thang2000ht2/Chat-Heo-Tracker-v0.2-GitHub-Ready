package vn.chatheotracker;
import java.util.*;
public final class TestMonteCarloAdvisor {
    private static void ok(boolean b,String s){if(!b)throw new AssertionError(s);}
    private static List<String> cards(String s){return Arrays.asList(s.split(" +"));}
    public static void main(String[] args){
        MonteCarloAdvisor.Advice a=MonteCarloAdvisor.advise(cards("3S 4S 5S 6C 6H AS 2H"),Collections.emptyList(),Collections.emptyList(),new int[]{-1,-1,-1});
        ok(a.title.startsWith("Gợi ý") && a.detail.contains("Chưa mô phỏng"),"fallback on unknown counters");
        MonteCarloAdvisor.Advice stale=MonteCarloAdvisor.advise(cards("3S 4S"),Collections.emptyList(),cards("3S"),new int[]{10,10,10});
        ok(stale.title.contains("chưa được cập nhật"),"must reject stale own hand");
        MonteCarloAdvisor.Advice overlap=MonteCarloAdvisor.advise(cards("3S 4H"),cards("3S"),Collections.emptyList(),new int[]{-1,-1,-1});
        ok(overlap.title.contains("trùng"),"must reject hand table overlap");
        MonteCarloAdvisor.Advice illegal=MonteCarloAdvisor.advise(cards("3S 4S"),cards("7S 7C"),Collections.emptyList(),null);
        ok(illegal.title.contains("Chưa có nước"),"should not suggest illegal beats");
        MonteCarloAdvisor.Advice invalidLead=MonteCarloAdvisor.advise(cards("3S 4S"),cards("8S 10S"),Collections.emptyList(),null);
        ok(invalidLead.title.contains("Chưa đọc rõ"),"reject invalid table move");
        // Known 13-card hand, all 3 opponent counts 13: this is a mathematically consistent deal.
        List<String> sample=cards("3S 3C 4S 4C 5S 5C 6S 7S 8S 9S 10S JS 2H");
        long before=System.nanoTime();
        MonteCarloAdvisor.Advice mc=MonteCarloAdvisor.advise(sample,Collections.emptyList(),Collections.emptyList(),new int[]{13,13,13});
        double secs=(System.nanoTime()-before)/1e9;
        ok(mc.detail.contains("192 mẫu"),"sampling works with exact 52-card partition");
        ok(mc.detail.contains("có bài để chặn"),"risk present");
        ok(mc.detail.contains("Không phải xác suất thắng"),"honest uncertainty label");
        ok(mc.detail.contains("Ai có bài để chặn gợi ý"),"opponent-specific threat analysis");
        ok(secs<15,"sampling unexpectedly slow: "+secs);
        // Property-style randomized test with consistent 13/13/13/13 deals.
        ArrayList<String> deck=new ArrayList<>();
        for(String r:MonteCarloAdvisor.RANKS)for(String suit:MonteCarloAdvisor.SUITS)deck.add(r+suit);
        Random rng=new Random(20261017L);
        for(int trial=0;trial<40;trial++){
            Collections.shuffle(deck,rng);
            List<String> current=new ArrayList<>(deck.subList(0,13));
            MonteCarloAdvisor.Advice sampleResult=MonteCarloAdvisor.advise(current,
                Collections.emptyList(),Collections.emptyList(),new int[]{13,13,13});
            ok(sampleResult.title.startsWith("Gợi ý"),"random trial "+trial+" "+sampleResult.title);
            ok(sampleResult.detail.contains("192 mẫu"),"random model did not run at "+trial);
        }
        System.out.println("TestMonteCarloAdvisor OK ("+String.format(Locale.US,"%.2f",secs)+" seconds sample)");
    }
}
