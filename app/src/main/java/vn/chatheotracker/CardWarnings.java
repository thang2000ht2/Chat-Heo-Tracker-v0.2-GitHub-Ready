package vn.chatheotracker;

import java.util.Collection;
import java.util.HashSet;

/** Pure Java: warn about POSSIBLE pairs in all unseen cards, not an opponent's hand. */
final class CardWarnings {
    static final String[] RANKS={"3","4","5","6","7","8","9","10","J","Q","K","A","2"};
    private static final String[] SUITS={"S","C","D","H"};
    static final class Result {
        final int[] remainingByRank=new int[RANKS.length];
        final boolean[] inTriple=new boolean[RANKS.length];
        final boolean[] inQuad=new boolean[RANKS.length];
        boolean hasTriple,hasQuad;
    }
    static Result analyze(Collection<String> cards) {
        Result out=new Result();
        HashSet<String> unseen=new HashSet<>(cards);
        for(int i=0;i<RANKS.length;i++){
            for(String suit:SUITS)if(unseen.contains(RANKS[i]+suit))out.remainingByRank[i]++;
        }
        // A '2' cannot participate in a consecutive-pairs run in this rule set.
        for(int len=3;len<=4;len++){
            for(int start=0;start+len<=RANKS.length-1;start++){
                boolean possible=true;
                for(int i=start;i<start+len;i++){
                    if(out.remainingByRank[i]<2){possible=false;break;}
                }
                if(!possible)continue;
                if(len==3){
                    out.hasTriple=true;
                    for(int i=start;i<start+len;i++)out.inTriple[i]=true;
                }else{
                    out.hasQuad=true;
                    for(int i=start;i<start+len;i++)out.inQuad[i]=true;
                }
            }
        }
        return out;
    }
    private CardWarnings(){}
}
