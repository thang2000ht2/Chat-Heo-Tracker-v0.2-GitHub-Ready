package vn.chatheotracker;

import java.util.*;

/** Conservative rescue of a six-card play whose complete WHITE layer was visible
 * briefly, while the temporal fragment matcher missed part of the animation.
 * Never guess hidden cards or infer an opponent from a transient raw counter.
 */
@Deprecated
final class SixCardLeadRecovery {
    static final class Drop {
        final int seat, delta; final long at;
        Drop(int seat,int delta,long at){this.seat=seat;this.delta=delta;this.at=at;}
    }
    static final class Result {
        final int dropIndex,seat; final List<String> cards;
        Result(int dropIndex,int seat,Collection<String> cards){
            this.dropIndex=dropIndex;this.seat=seat;
            this.cards=Collections.unmodifiableList(new ArrayList<>(cards));
        }
    }
    static Result match(Collection<String> white,List<Drop> drops,
                        Collection<String> recorded,Collection<String> myHand,long now){
        if(white==null||drops==null||white.size()!=6)return null;
        LinkedHashSet<String> cards=new LinkedHashSet<>(white);
        if(cards.size()!=6 || !legalSix(cards))return null;
        if(recorded!=null&&!Collections.disjoint(cards,recorded))return null;
        if(myHand!=null&&!Collections.disjoint(cards,myHand))return null;
        int eligible=-1;
        for(int i=0;i<drops.size();i++){
            Drop d=drops.get(i);
            if(d.delta!=6 || d.seat<0 || d.seat>2 || Math.abs(now-d.at)>4200)continue;
            if(eligible>=0)return null; // ambiguous actor: do not learn this move
            eligible=i;
        }
        return eligible<0?null:new Result(eligible,drops.get(eligible).seat,cards);
    }
    /** 3 consecutive pairs (including 8-9-10), or a six-card straight without 2. */
    static boolean legalSix(Collection<String> cards){
        if(cards==null||cards.size()!=6)return false;
        int[] counts=new int[13];
        Set<String> seen=new HashSet<>();
        for(String card:cards){
            if(card==null || card.length()<2 || !seen.add(card))return false;
            String rank=card.substring(0,card.length()-1);
            int r=rankIndex(rank);
            char suit=card.charAt(card.length()-1);
            if(r<0 || "SHDC".indexOf(suit)<0 || ++counts[r]>4)return false;
        }
        int min=13,max=-1,distinct=0;
        for(int i=0;i<13;i++)if(counts[i]>0){min=Math.min(min,i);max=Math.max(max,i);distinct++;}
        if(max==12)return false; // Neither 3 consecutive pairs nor straight may include a two.
        if(distinct==3 && max-min==2){
            for(int i=min;i<=max;i++)if(counts[i]!=2)return false;
            return true;
        }
        if(distinct==6 && max-min==5){
            for(int i=min;i<=max;i++)if(counts[i]!=1)return false;
            return true;
        }
        return false;
    }
    private static int rankIndex(String rank){
        String[] names={"3","4","5","6","7","8","9","10","J","Q","K","A","2"};
        for(int i=0;i<names.length;i++)if(names[i].equals(rank))return i;
        return -1;
    }
    private SixCardLeadRecovery(){}
}
