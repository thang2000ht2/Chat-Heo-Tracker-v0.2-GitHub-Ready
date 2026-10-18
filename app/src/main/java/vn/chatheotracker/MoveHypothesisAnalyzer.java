package vn.chatheotracker;

import java.util.*;

/** Considers all legal public move structures compatible with an observed
 * partial ACTIVE layer and a verified card-count drop. This is feasibility,
 * not a calibrated probability and never identifies unseen suits/cards.
 */
final class MoveHypothesisAnalyzer {
    static final class Hypothesis {
        final String shape, ranks;
        final int seen,total;
        Hypothesis(String shape,String ranks,int seen,int total){
            this.shape=shape;this.ranks=ranks;this.seen=seen;this.total=total;
        }
        @Override public String toString(){return shape+" "+ranks+" "+seen+"/"+total;}
    }
    static List<Hypothesis> possible(int target,Collection<String> visible,
                                      Collection<String> excluded){
        if(target<1||target>13||visible==null||visible.size()>target)return Collections.emptyList();
        Set<String> seen=new LinkedHashSet<>(visible),removed=new HashSet<>();
        if(excluded!=null)removed.addAll(excluded);
        if(seen.size()!=visible.size()||!Collections.disjoint(seen,removed))return Collections.emptyList();
        int[] v=new int[13],available=new int[13];
        for(int i=0;i<13;i++)for(char suit:new char[]{'S','H','D','C'})
            if(!removed.contains(PlayStructure.RANKS[i]+suit))available[i]++;
        for(String card:seen){
            if(card==null||card.length()<2)return Collections.emptyList();
            String rank=card.substring(0,card.length()-1);int idx=-1;
            for(int i=0;i<13;i++)if(PlayStructure.RANKS[i].equals(rank)){idx=i;break;}
            if(idx<0||"SHDC".indexOf(card.charAt(card.length()-1))<0)return Collections.emptyList();
            v[idx]++;
        }
        ArrayList<Hypothesis> ans=new ArrayList<>();
        if(target<=4){
            int[] needed=new int[13];
            for(int r=0;r<13;r++){
                Arrays.fill(needed,0);needed[r]=target;
                if(feasible(v,available,needed))
                    ans.add(new Hypothesis(new String[]{"","SINGLE","PAIR","TRIPLE","FOUR_KIND"}[target],
                        PlayStructure.RANKS[r],seen.size(),target));
            }
        }
        // Run length equals the number of cards; straights cannot include 2.
        if(target>=3&&target<=12){
            for(int begin=0;begin+target<=12;begin++){
                int[] needed=new int[13];
                for(int j=0;j<target;j++)needed[begin+j]=1;
                if(feasible(v,available,needed))ans.add(new Hypothesis("STRAIGHT",
                    PlayStructure.RANKS[begin]+"-"+PlayStructure.RANKS[begin+target-1],seen.size(),target));
            }
        }
        if(target==6 || target==8){
            int length=target/2;
            for(int begin=0;begin+length<=12;begin++){
                int[] needed=new int[13];
                for(int j=0;j<length;j++)needed[begin+j]=2;
                if(feasible(v,available,needed))ans.add(new Hypothesis(
                    length==3?"THREE_PAIRS":"FOUR_PAIRS",
                    PlayStructure.RANKS[begin]+"-"+PlayStructure.RANKS[begin+length-1],seen.size(),target));
            }
        }
        return ans;
    }
    private static boolean feasible(int[] seen,int[] available,int[] expected){
        for(int i=0;i<13;i++)
            if(seen[i]>expected[i]||expected[i]>available[i])return false;
        return true;
    }
    private MoveHypothesisAnalyzer(){}
}
