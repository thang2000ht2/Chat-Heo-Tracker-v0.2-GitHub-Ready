package vn.chatheotracker;

import java.util.*;
import java.util.regex.*;

/** Experimental, offline, rule-based move suggestions. Not a trained AI model.
 * Assumes usual southern Tien Len groups. House rules and special chops are
 * intentionally excluded; suggestions are advisory only. */
final class StrategyAdvisor {
    private static final String[] RANKS={"3","4","5","6","7","8","9","10","J","Q","K","A","2"};
    private static final String[] SUITS={"S","C","D","H"};
    private static final Pattern TOKEN=Pattern.compile("(?i)(10|[2-9JQKA])([SCDH♠♣♦♥])");
    static final class Input {
        final LinkedHashSet<String> cards;
        final String error;
        Input(LinkedHashSet<String> c,String e){cards=c;error=e;}
    }
    static Input parse(String raw){
        LinkedHashSet<String> cards=new LinkedHashSet<>();
        if(raw==null||raw.trim().isEmpty())return new Input(cards,"Chưa nhập lá bài.");
        String s=raw.toUpperCase(Locale.ROOT).replaceAll("[\\s,;|/]+", " ").trim();
        for(String token:s.split(" +")){
            Matcher m=TOKEN.matcher(token);
            if(!m.matches())return new Input(cards,"Sai ký hiệu: "+token+". Ví dụ 6♠ 6♥ 10D AH");
            String suit=m.group(2);
            switch(suit){case "♠":suit="S";break;case "♣":suit="C";break;case "♦":suit="D";break;case "♥":suit="H";break;}
            String card=m.group(1)+suit;
            if(!cards.add(card))return new Input(cards,"Trùng lá: "+pretty(card));
            if(cards.size()>13)return new Input(cards,"Tay bài không thể vượt quá 13 lá.");
        }
        return new Input(cards,null);
    }
    static final class Result {
        final String title,detail;
        Result(String t,String d){title=t;detail=d;}
    }
    private static final class Move {
        final ArrayList<String> cards;
        final String type;
        final int high;
        Move(String type,Collection<String> cards,int high){this.type=type;this.cards=new ArrayList<>(cards);this.high=high;}
    }
    static Result suggest(Collection<String> hand,Collection<String> onTable,int[] opponents){
        if(hand==null||hand.isEmpty())return new Result("Chưa có bài tay", "Mở Tracker để nhập các lá bài hiện tại rồi bấm Gợi ý.");
        ArrayList<Move> moves=generate(hand);
        if(moves.isEmpty())return new Result("Không xác định được nước", "Kiểm tra lại danh sách bài tay.");
        String danger=threat(opponents);
        if(onTable!=null&&!onTable.isEmpty()){
            Move lead=classify(onTable);
            if(lead==null)return new Result("Không đủ dữ liệu bài giữa bàn", "Chưa thể đề xuất nước chặn an toàn. "+danger);
            Move best=null;double bestScore=-1e9;
            for(Move move:moves){
                if(!move.type.equals(lead.type)||move.cards.size()!=lead.cards.size())continue;
                if(move.high<=lead.high)continue; // conservative: avoids unsupported chopping exceptions
                double score=-move.high*5-splitPenalty(move,hand)+move.cards.size()*3;
                if(score>bestScore){best=move;bestScore=score;}
            }
            if(best==null)return new Result("Chưa có nước chặn cùng loại", "Có thể bỏ lượt; chưa xét luật chặt heo/tứ quý/đôi thông. "+danger);
            return new Result("Cân nhắc chặn: "+render(best.cards),"Ưu tiên bộ vừa đủ mạnh, hạn chế phá bài lớn. "+danger);
        }
        Move best=null;double bestScore=-1e9;
        for(Move m:moves){
            double score=m.cards.size()*22-m.high*2.4-splitPenalty(m,hand);
            if(m.type.equals("single")&&m.high==12)score-=40;
            if(m.type.equals("four"))score-=52; // retain the chop if possible
            if(score>bestScore){bestScore=score;best=m;}
        }
        if(best==null)return new Result("Chưa có gợi ý", "Kiểm tra bài tay.");
        return new Result("Cân nhắc ra: "+render(best.cards),"Gợi ý MỞ LƯỢT: ưu tiên giải phóng bài, hạn chế phá bộ mạnh. "+danger);
    }
    private static double splitPenalty(Move move,Collection<String> hand){
        int[] total=countRanks(hand),used=countRanks(move.cards);
        double penalty=0;
        for(int i=0;i<13;i++){
            if(used[i]==0)continue;
            if(total[i]>=2 && used[i]<total[i])penalty+=13.0;
            if(i==12)penalty+=12.0;
        }
        return penalty;
    }
    private static String threat(int[] counts){
        int min=14;
        if(counts!=null)for(int n:counts)if(n>=1&&n<=13)min=Math.min(min,n);
        if(min<=2)return "Cảnh báo: đối thủ chỉ còn "+min+" lá.";
        if(min<=5)return "Một đối thủ còn "+min+" lá.";
        return "Chỉ mang tính tham khảo; không biết bài úp của đối thủ.";
    }
    private static int rank(String card){
        String r=card.substring(0,card.length()-1);
        for(int i=0;i<RANKS.length;i++)if(RANKS[i].equals(r))return i;
        return -1;
    }
    private static int suit(String card){
        String s=card.substring(card.length()-1);
        for(int i=0;i<SUITS.length;i++)if(SUITS[i].equals(s))return i;
        return -1;
    }
    private static int[] countRanks(Collection<String> hand){
        int[] a=new int[13];for(String c:hand){int r=rank(c);if(r>=0)a[r]++;}return a;
    }
    private static ArrayList<Move> generate(Collection<String> hand){
        ArrayList<Move> out=new ArrayList<>();
        ArrayList<String>[] byRank=new ArrayList[13];
        for(int i=0;i<13;i++)byRank[i]=new ArrayList<>();
        for(String c:hand){int r=rank(c);if(r>=0)byRank[r].add(c);}
        for(int r=0;r<13;r++){
            Collections.sort(byRank[r],(a,b)->Integer.compare(suit(a),suit(b)));
            int n=byRank[r].size();
            for(int k=1;k<=Math.min(4,n);k++){
                // choose lower suits for tie-breaking; special four only when complete
                if(k==4&&n!=4)continue;
                out.add(new Move(k==1?"single":k==2?"pair":k==3?"triple":"four",byRank[r].subList(0,k),r));
            }
        }
        // Straight: 3+ consecutive ranks, 2 cannot appear. Only use one card per rank.
        for(int start=0;start<12;start++){
            ArrayList<String> run=new ArrayList<>();
            for(int r=start;r<12&&byRank[r].size()>0;r++){
                run.add(byRank[r].get(0));
                if(run.size()>=3)out.add(new Move("straight",run,r));
            }
        }
        // Three/four consecutive pairs, when available. 2 excluded.
        for(int start=0;start<12;start++){
            ArrayList<String> run=new ArrayList<>();
            for(int r=start;r<12&&byRank[r].size()>=2;r++){
                run.add(byRank[r].get(0));run.add(byRank[r].get(1));
                int pairs=run.size()/2;
                if(pairs==3||pairs==4)out.add(new Move("pairs"+pairs,run,r));
                if(pairs>=4)break;
            }
        }
        return out;
    }
    private static Move classify(Collection<String> cards){
        if(cards.isEmpty())return null;
        ArrayList<String> cs=new ArrayList<>(cards);
        int[] c=countRanks(cs); int distinct=0,max=-1,minimum=13;
        for(int i=0;i<13;i++)if(c[i]>0){distinct++;max=i;minimum=Math.min(minimum,i);}
        if(distinct==1){String t=cs.size()==1?"single":cs.size()==2?"pair":cs.size()==3?"triple":cs.size()==4?"four":"?";return new Move(t,cs,max);}
        if(cs.size()>=3 && cs.size()==distinct && max<12 && max-minimum+1==distinct)return new Move("straight",cs,max);
        if(cs.size()%2==0&&distinct>=3&&distinct<=4&&max<12&&max-minimum+1==distinct){
            for(int x:c)if(x!=0&&x!=2)return null;
            return new Move("pairs"+distinct,cs,max);
        }
        return null;
    }
    private static String render(Collection<String> cards){
        StringBuilder b=new StringBuilder();
        for(String c:cards){if(b.length()>0)b.append(' ');b.append(pretty(c));}
        return b.toString();
    }
    private static String pretty(String c){return c.substring(0,c.length()-1)+RecentMoves.symbol(c.substring(c.length()-1));}
    private StrategyAdvisor(){}
}
