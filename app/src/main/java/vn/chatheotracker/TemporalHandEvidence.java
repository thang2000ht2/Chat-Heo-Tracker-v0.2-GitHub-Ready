package vn.chatheotracker;

import java.util.*;

/** Fuses identified card corners from successive views of the SAME fan.
 * Unknown/occluded positions are not guessed. A full distinct set is emitted
 * only after each indexed slot has independent repeated visual evidence.
 */
final class TemporalHandEvidence {
    private int expected=-1, origin=-10000, frames=0;
    private final ArrayList<HashMap<String,Integer>> votes=new ArrayList<>();
    void clear(){expected=-1;origin=-10000;frames=0;votes.clear();}
    List<String> accept(List<String> slots,int size,int left){
        if(size<1||size>13 || slots==null||slots.size()!=size){clear();return Collections.emptyList();}
        if(expected!=size || Math.abs(origin-left)>14 || frames>=24){
            clear();expected=size;origin=left;
            for(int i=0;i<size;i++)votes.add(new HashMap<>());
        }
        frames++;
        for(int i=0;i<size;i++){
            String card=slots.get(i);
            if(card==null || MonteCarloAdvisor.rank(card)<0 || MonteCarloAdvisor.suit(card)<0)continue;
            HashMap<String,Integer> tally=votes.get(i);
            tally.put(card,tally.getOrDefault(card,0)+1);
        }
        ArrayList<String> result=new ArrayList<>();
        for(HashMap<String,Integer> tally:votes){
            String winner=null;int first=0,second=0;
            for(Map.Entry<String,Integer> e:tally.entrySet()){
                int v=e.getValue();
                if(v>first){second=first;first=v;winner=e.getKey();}
                else if(v>second)second=v;
            }
            if(winner==null || first<3 || first-second<2)return Collections.emptyList();
            result.add(winner);
        }
        if(new HashSet<>(result).size()!=size)return Collections.emptyList();
        return result;
    }
}
