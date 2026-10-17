package vn.chatheotracker;
import java.util.*;
public final class TestLowestBlockRisk {
    static void check(boolean ok,String msg){if(!ok)throw new AssertionError(msg);}
    static List<String> split(String s){return Arrays.asList(s.split(" +"));}
    public static void main(String[] args){
        List<String> mine=split("3S 3C 4H 5S 5C 6S 7H 8H 9S 10C JC QC 2H");
        int[] initial={13,13,13};
        MonteCarloAdvisor.Advice open=MonteCarloAdvisor.adviseLowestBlockRisk(
            mine,Collections.emptyList(),Collections.emptyList(),initial);
        check(open.title.startsWith("ĐÁNH "),"must suggest a concrete play: "+open.title);
        check(open.detail.contains("chưa hiệu chuẩn"),"must label sampled response risk");
        check(!open.detail.contains("Chế độ:")&&!open.detail.contains("lượt tối thiểu"),"no lengthy tactical prose");
        MonteCarloAdvisor.Advice mismatch=MonteCarloAdvisor.adviseLowestBlockRisk(
            mine,Collections.emptyList(),Collections.emptyList(),new int[]{12,13,13});
        check(mismatch.title.contains("QUÉT LẠI"),"counts mismatch must rescan");
        MonteCarloAdvisor.Advice stale=MonteCarloAdvisor.adviseLowestBlockRisk(
            mine,Collections.emptyList(),split("3S"),new int[]{12,13,13});
        check(stale.title.contains("QUÉT LẠI"),"hand overlaps played");
        double risk=MonteCarloAdvisor.chopRisk(mine,Collections.emptyList(),initial);
        check(risk>=0&&risk<=1,"chop samples within bounds");
        check(MonteCarloAdvisor.chopRisk(split("3S 4C"),Collections.emptyList(),new int[]{-1,-1,-1})<0,
            "no heo should not cause alert");
        List<String> lead=split("4S 4C");
        MonteCarloAdvisor.Advice response=MonteCarloAdvisor.adviseLowestBlockRisk(
            mine,lead,lead,new int[]{13,13,11});
        check(response.title.startsWith("ĐÁNH "),"valid pair lead must be beatable; check classified count: "+response.title);
        MonteCarloAdvisor.Advice noReply=MonteCarloAdvisor.adviseLowestBlockRisk(
            split("3S 4C"),split("AS"),split("AS"),new int[]{-1,-1,-1});
        check(noReply.title.contains("BỎ LƯỢT")||noReply.title.contains("QUÉT LẠI"),"no illegal lead response");
        // Fuzz 40 legal 13/13/13/13 deals. The advice must remain concise.
        ArrayList<String> deck=new ArrayList<>();
        for(String r:MonteCarloAdvisor.RANKS)for(String suit:MonteCarloAdvisor.SUITS)deck.add(r+suit);
        Random rand=new Random(20261017L);
        for(int i=0;i<40;i++){
            Collections.shuffle(deck,rand);
            List<String> h=new ArrayList<>(deck.subList(0,13));
            MonteCarloAdvisor.Advice a=MonteCarloAdvisor.adviseLowestBlockRisk(
                h,Collections.emptyList(),Collections.emptyList(),new int[]{13,13,13});
            check(a.title.startsWith("ĐÁNH "),"random deal must produce a move ("+i+") " +a.title);
            check(a.detail.contains("chưa hiệu chuẩn") && !a.detail.contains("%"),"concise only ("+i+")");
        }
        System.out.println("TestLowestBlockRisk OK");
    }
}
