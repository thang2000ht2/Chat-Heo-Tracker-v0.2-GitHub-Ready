package vn.chatheotracker;

import java.util.*;

/** Maintains the opponent's chronological play history (oldest on left). */
final class RecentMoves {
    private final ArrayList<LinkedHashSet<String>> chronological = new ArrayList<>();

    void record(Collection<String> cards) {
        LinkedHashSet<String> unique=new LinkedHashSet<>();
        for(String c:cards)if(isCard(c))unique.add(c);
        if(!unique.isEmpty())chronological.add(unique);
    }
    void remove(String card){
        for(int i=chronological.size()-1;i>=0;i--){
            LinkedHashSet<String> move=chronological.get(i);
            move.remove(card);
            if(move.isEmpty())chronological.remove(i);
        }
    }
    /** Detached immutable-looking snapshot for off-thread advice. */
    List<List<String>> snapshot(){
        ArrayList<List<String>> copy=new ArrayList<>();
        for(Collection<String> move:chronological)copy.add(new ArrayList<>(move));
        return copy;
    }
    void clear(){chronological.clear();}
    boolean isEmpty(){return chronological.isEmpty();}
    int size(){return chronological.size();}
    String mostRecent(){return chronological.isEmpty()?"":formatMove(chronological.get(chronological.size()-1));}
    String olderMoves(){
        StringBuilder b=new StringBuilder();
        for(int i=0;i<chronological.size()-1;i++){
            if(b.length()>0)b.append("  ›  ");
            b.append(formatMove(chronological.get(i)));
        }
        return b.toString();
    }
    String fullText(){
        String old=olderMoves(),latest=mostRecent();
        return old.isEmpty()?latest:old+"  ›  "+latest;
    }
    private static String formatMove(Collection<String> cards){
        LinkedHashMap<String,LinkedHashSet<String>> ranks=new LinkedHashMap<>();
        for(String card:cards){
            if(!isCard(card))continue;
            String r=card.substring(0,card.length()-1);
            ranks.computeIfAbsent(r,k->new LinkedHashSet<>()).add(card.substring(card.length()-1));
        }
        StringBuilder out=new StringBuilder();
        for(Map.Entry<String,LinkedHashSet<String>> entry:ranks.entrySet()){
            if(out.length()>0)out.append(' ');
            out.append(entry.getKey());
            for(String s:entry.getValue())out.append(symbol(s));
        }
        return out.toString();
    }
    static boolean isCard(String c){
        if(c==null||c.length()<2)return false;
        String rank=c.substring(0,c.length()-1),suit=c.substring(c.length()-1);
        return Arrays.asList("3","4","5","6","7","8","9","10","J","Q","K","A","2").contains(rank)
            && Arrays.asList("S","C","D","H").contains(suit);
    }
    static String symbol(String s){
        switch(s){case "S":return "♠";case "C":return "♣";case "D":return "♦";case "H":return "♥";default:return "?";}
    }
}
