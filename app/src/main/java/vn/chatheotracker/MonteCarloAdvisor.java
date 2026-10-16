package vn.chatheotracker;

import java.util.*;

/**
 * On-demand information-set sampling + exact minimum-turn partition for the visible hand.
 * No pretrained neural weights; scores are relative heuristics, NEVER win probabilities.
 * Chặt / penalties are proxy costs until verified against the game's actual rule table.
 * Plain Java on purpose: can be fuzz-tested without an Android emulator.
 */
final class MonteCarloAdvisor {
    static final String[] RANKS={"3","4","5","6","7","8","9","10","J","Q","K","A","2"};
    static final String[] SUITS={"S","C","D","H"};
    static final int DEALS=192;
    static final class Advice {
        final String title, detail;
        Advice(String t,String d){title=t;detail=d;}
    }
    static final class Move {
        final int mask, highRank, highSuit;
        final String kind;
        final int count;
        final boolean containsTwo;
        Move(int bits,String k,int hi,int suit,boolean two){mask=bits;kind=k;highRank=hi;highSuit=suit;count=Integer.bitCount(bits);containsTwo=two;}
    }
    private static final class Choice {
        final Move move;
        double score, response, chop;
        final double[] responseByPlayer=new double[3];
        Choice(Move m){move=m;}
    }
    private final ArrayList<String> hand;
    private final ArrayList<Move> moves=new ArrayList<>();
    private final int[] minTurns;
    private MonteCarloAdvisor(Collection<String> mine){
        hand=new ArrayList<>(mine);
        hand.sort((a,b)->{int n=rank(a)-rank(b);return n!=0?n:suit(a)-suit(b);});
        int size=1<<hand.size();
        minTurns=new int[size];
        Arrays.fill(minTurns,99);minTurns[0]=0;
        for(int m=1;m<size;m++){
            Move mv=classifyMask(m);
            if(mv!=null)moves.add(mv);
        }
        for(int state=1;state<size;state++){
            // Every valid subset can be one play; exact minimal count to empty the hand.
            for(Move m:moves){
                if((m.mask&state)==m.mask){
                    int v=minTurns[state^m.mask]+1;
                    if(v<minTurns[state])minTurns[state]=v;
                }
            }
        }
    }
    static Advice advise(Collection<String> myHand, Collection<String> table,
                         Collection<String> seen, int[] counts){
        if(myHand==null||myHand.isEmpty())return new Advice("Chưa có bài trên tay", "Nhập bài hiện tại trong Tracker rồi bấm Gợi ý. Không suy đoán bài tay từ dữ liệu thiếu.");
        if(myHand.size()>13)return new Advice("Dữ liệu bài tay không hợp lệ", "Tối đa 13 lá trong một tay.");
        Set<String> uniq=new HashSet<>(myHand);
        if(uniq.size()!=myHand.size()||!validCards(uniq))return new Advice("Sai dữ liệu lá bài", "Kiểm tra quân bài trùng hoặc ký hiệu sai.");
        if(seen!=null){
            Set<String> overlap=new HashSet<>(myHand);overlap.retainAll(seen);
            if(!overlap.isEmpty())return new Advice("Bài tay chưa được cập nhật", "Có "+overlap.size()+" lá đã ghi nhận đánh nhưng vẫn có trong tay. Hãy sửa bài tay hiện tại trước khi gợi ý.");
        }
        if(table!=null){for(String c:table)if(uniq.contains(c))return new Advice("Bài cần chặn trùng bài tay", "Vui lòng chỉnh lại bộ bài giữa bàn.");}
        if(table!=null && !table.isEmpty() && (seen==null||!seen.containsAll(table))){
            // Table can be live before the recognizer has recorded it; continue, but don't
            // treat the table cards as hidden cards in sampling.
        }
        MonteCarloAdvisor engine=new MonteCarloAdvisor(myHand);
        return engine.compute(table,seen,counts);
    }
    private Advice compute(Collection<String> table,Collection<String> seen,int[] counts){
        Move toBeat=(table!=null && !table.isEmpty())?classifyCards(table):null;
        if(table!=null && !table.isEmpty() && toBeat==null)
            return new Advice("Chưa đọc rõ bài giữa bàn", "Không đưa gợi ý đánh khi chưa xác nhận được bộ bài cần chặn hoặc luật chặt.");
        ArrayList<Choice> choices=new ArrayList<>();
        for(Move m:moves){
            if(toBeat!=null && !beats(m,toBeat))continue;
            choices.add(new Choice(m));
        }
        if(choices.isEmpty())return new Advice("Chưa có nước chặn an toàn", "Không tìm thấy bộ lớn hơn cùng loại. Chưa xét luật chặt đặc biệt; cân nhắc BỎ LƯỢT.");

        boolean sampling=false;
        ArrayList<String> hidden=new ArrayList<>();
        int sum=0;
        boolean validCounts=counts!=null && counts.length==3;
        if(validCounts){
            for(int n:counts){if(n<0||n>13)validCounts=false;else sum+=n;}
        }
        Set<String> excluded=new HashSet<>(hand);
        if(seen!=null)excluded.addAll(seen);
        if(table!=null)excluded.addAll(table);
        for(String r:RANKS)for(String s:SUITS){String card=r+s;if(!excluded.contains(card))hidden.add(card);}
        if(validCounts && sum==hidden.size())sampling=true;

        final int full=(1<<hand.size())-1;
        Random random=new Random(seed(hand,table,seen,counts));
        final int deals=sampling?DEALS:0;
        double[] answered=new double[choices.size()],chopped=new double[choices.size()];
        if(sampling){
            for(int trial=0;trial<deals;trial++){
                Collections.shuffle(hidden,random);
                int[][] opp=new int[3][13];
                int[][] highestSuits=new int[3][13];
                for(int k=0;k<3;k++)Arrays.fill(highestSuits[k],-1);
                int p=0;
                for(int o=0;o<3;o++)for(int j=0;j<counts[o];j++){
                    String card=hidden.get(p++);
                    int r=rank(card);opp[o][r]++;
                    highestSuits[o][r]=Math.max(highestSuits[o][r],suit(card));
                }
                for(int c=0;c<choices.size();c++){
                    Move m=choices.get(c).move;
                    boolean any=false,anyChop=false;
                    for(int o=0;o<3;o++){
                        if(counts[o]==0)continue;
                        if(canReply(opp[o],highestSuits[o],m)){
                            any=true;
                            choices.get(c).responseByPlayer[o]++;
                        }
                        if(m.containsTwo && canChopTwo(opp[o],m))anyChop=true;
                    }
                    if(any)answered[c]++;
                    if(anyChop)chopped[c]++;
                }
            }
        }
        int minOpp=14;
        if(counts!=null)for(int n:counts)if(n>0&&n<=13)minOpp=Math.min(minOpp,n);
        for(int i=0;i<choices.size();i++){
            Choice c=choices.get(i);
            Move m=c.move;
            int rem=full^m.mask;
            int remainingCards=hand.size()-m.count;
            // Low future play count is more important than raw number of cards discarded.
            double score=85-15.0*minTurns[rem] + 1.6*m.count;
            // Keep reliable control cards as a way back into the lead.
            double control=0;
            for(int j=0;j<hand.size();j++)if((rem&(1<<j))!=0){
                int r=rank(hand.get(j));if(r>=10)control+=r==12?3.5:1.4;
            }
            score+=Math.min(8,control);
            if(sampling){
                c.response=answered[i]/deals;
                c.chop=chopped[i]/deals;
                score-=21*c.response;
                if(m.containsTwo)score-=27*c.chop;  // money/penalty exposure proxy
            }else{
                // Declared explicitly as heuristic when the counters are incomplete.
                score-=m.highRank*0.75;
                if(m.containsTwo)score-=7;
            }
            // Discarding an unnecessary 2 too early has monetary and tempo risk.
            if(m.containsTwo && remainingCards>=5)score-=7;
            // Protect higher-cost leftover 2s when an opponent can finish immediately.
            if(minOpp<=2){
                for(int j=0;j<hand.size();j++)if((rem&(1<<j))!=0&&rank(hand.get(j))==12)score-=7;
                if(m.kind.equals("single")&&remainingCards>=3)score-=7;
            }
            // A quartet is a valuable chop: avoid spending it unless it clears the hand
            // or the next opponent is close to finishing.
            if(m.kind.equals("four")&&remainingCards>0&&minOpp>3)score-=10;
            c.score=score;
        }
        choices.sort((a,b)->{int compare=Double.compare(b.score,a.score);return compare!=0?compare:Integer.compare(b.move.count,a.move.count);});
        StringBuilder detail=new StringBuilder();
        int limit=Math.min(3,choices.size());
        for(int i=0;i<limit;i++){
            Choice c=choices.get(i);
            if(i>0)detail.append("\n");
            detail.append(i+1).append(". ").append(render(c.move)).append(" · còn ")
                .append(minTurns[full^c.move.mask]).append(" lượt tối thiểu");
            if(sampling)detail.append(" · có bài để chặn: ").append((int)Math.round(100*c.response)).append("% mẫu");
        }
        detail.append("\n");
        if(sampling){
            detail.append(DEALS).append(" mẫu chia bài · cân bằng về trước và rủi ro chặt/phạt (ước lượng)");
            Choice leader=choices.get(0);
            detail.append("\nAi có bài để chặn gợi ý: trên ")
                  .append((int)Math.round(leader.responseByPlayer[0]*100/DEALS)).append("% · trái ")
                  .append((int)Math.round(leader.responseByPlayer[1]*100/DEALS)).append("% · phải ")
                  .append((int)Math.round(leader.responseByPlayer[2]*100/DEALS)).append("% mẫu.");
        }else{
            detail.append("Chưa mô phỏng: thiếu/sai số đếm. Cần tổng bài úp đối thủ = số quân chưa biết (")
                  .append(sum).append("/").append(hidden.size()).append(").");
        }
        if(minOpp<=2)detail.append("\nChú ý: đối thủ còn ").append(minOpp).append(" lá.");
        detail.append("\nKhông phải xác suất thắng; luật chặt đặc biệt và tiền phạt thực tế chưa xác nhận.");
        return new Advice("Gợi ý: "+render(choices.get(0).move),detail.toString());
    }
    private Move classifyMask(int mask){
        int[] byRank=new int[13];
        int high=-1,highestSuit=0,low=13;
        for(int i=0;i<hand.size();i++)if((mask&(1<<i))!=0){
            String card=hand.get(i);int r=rank(card);if(r<0)return null;
            byRank[r]++;if(r>high){high=r;highestSuit=suit(card);}else if(r==high)highestSuit=Math.max(highestSuit,suit(card));low=Math.min(low,r);
        }
        return classified(mask,byRank,high,low,highestSuit);
    }
    private static Move classifyCards(Collection<String> cards){
        if(cards.isEmpty())return null;
        int[] byRank=new int[13];int hi=-1,lo=13,highSuit=0;
        for(String c:cards){int r=rank(c);if(r<0)return null;byRank[r]++;
            if(r>hi){hi=r;highSuit=suit(c);}else if(r==hi)highSuit=Math.max(highSuit,suit(c));lo=Math.min(lo,r);
        }
        return classified(0,byRank,hi,lo,highSuit);
    }
    private static Move classified(int mask,int[] ranks,int hi,int lo,int highSuit){
        if(hi<0)return null;
        int count=0,unique=0,groups2=0;
        for(int v:ranks){count+=v;if(v>0)unique++;if(v==2)groups2++;}
        if(unique==1){
            if(count>4)return null;
            return new Move(mask,count==1?"single":count==2?"pair":count==3?"triple":"four",hi,highSuit,hi==12);
        }
        if(hi==12)return null; // 2 never belongs in a sequence
        if(hi-lo+1!=unique)return null;
        if(count==unique && unique>=3)return new Move(mask,"straight",hi,highSuit,false);
        if(groups2==unique && unique>=3)return new Move(mask,"pairs"+unique,hi,highSuit,false);
        return null;
    }
    private static boolean beats(Move candidate,Move lead){
        if(!candidate.kind.equals(lead.kind)||candidate.count!=lead.count)return false;
        if(candidate.highRank!=lead.highRank)return candidate.highRank>lead.highRank;
        // Standard same-rank tie by highest suit is not uniformly implemented for all groups.
        // Conservatively permit suit tie-break only on a single, otherwise refuse to guess.
        return candidate.kind.equals("single")&&candidate.highSuit>lead.highSuit;
    }
    private static boolean canReply(int[] counts,int[] suits,Move move){
        switch(move.kind){
            case "single":
                for(int r=move.highRank;r<13;r++)if(counts[r]>0&&(r>move.highRank||suits[r]>move.highSuit))return true;
                return false;
            case "pair":case "triple":case "four":
                for(int r=move.highRank+1;r<13;r++)if(counts[r]>=move.count)return true;
                return false;
            case "straight":
                return consecutive(counts,1,move.count,move.highRank);
            default:
                if(move.kind.startsWith("pairs"))return consecutive(counts,2,move.count/2,move.highRank);
                return false;
        }
    }
    private static boolean consecutive(int[] ranks,int atLeast,int len,int beatsHigh){
        if(len>=13)return false;
        for(int end=beatsHigh+1;end<12;end++){
            if(end-len+1<0)continue;
            boolean ok=true;
            for(int r=end-len+1;r<=end;r++)if(ranks[r]<atLeast){ok=false;break;}
            if(ok)return true;
        }
        return false;
    }
    private static boolean canChopTwo(int[] opp,Move m){
        if(!m.containsTwo)return false;
        for(int n:opp)if(n==4)return true;
        int need=m.kind.equals("single")?3:m.kind.equals("pair")?4:6;
        return consecutive(opp,2,need,-1);
    }
    private static boolean validCards(Collection<String> cards){
        for(String c:cards)if(rank(c)<0||suit(c)<0)return false;
        return true;
    }
    static int rank(String card){
        if(card==null||card.length()<2)return -1;
        String name=card.substring(0,card.length()-1);
        for(int i=0;i<RANKS.length;i++)if(RANKS[i].equals(name))return i;
        return -1;
    }
    static int suit(String card){
        if(card==null||card.length()<2)return -1;
        String s=card.substring(card.length()-1);
        for(int i=0;i<SUITS.length;i++)if(SUITS[i].equals(s))return i;
        return -1;
    }
    private String render(Move m){
        StringBuilder b=new StringBuilder();
        for(int i=0;i<hand.size();i++)if((m.mask&(1<<i))!=0){
            String c=hand.get(i);if(b.length()>0)b.append(' ');
            b.append(c.substring(0,c.length()-1));
            b.append(RecentMoves.symbol(c.substring(c.length()-1)));
        }
        return b.toString();
    }
    private static long seed(Collection<String> hand,Collection<String> table,Collection<String> seen,int[] counts){
        ArrayList<String> h=new ArrayList<>(hand);Collections.sort(h);
        ArrayList<String> t=new ArrayList<>(table==null?Collections.emptyList():table);Collections.sort(t);
        ArrayList<String> s=new ArrayList<>(seen==null?Collections.emptyList():seen);Collections.sort(s);
        return Objects.hash(h,t,s,Arrays.toString(counts));
    }
    private MonteCarloAdvisor() {hand=null;minTurns=null;}
}
