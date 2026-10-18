package vn.chatheotracker;

import java.util.*;

/** Observable shapes, NOT Pig Hunters chopping legality. The game-specific
 * priorities, special cut chains, and penalties require verified rules.
 * This class is solely for deciding whether a batch could be one move.
 */
final class PlayStructure {
    static final String[] RANKS={"3","4","5","6","7","8","9","10","J","Q","K","A","2"};
    enum Kind { SINGLE, PAIR, TRIPLE, FOUR_KIND, STRAIGHT, THREE_PAIRS, FOUR_PAIRS, OTHER }
    static final class Shape {
        final Kind kind; final int size;
        Shape(Kind kind,int size){this.kind=kind;this.size=size;}
        boolean accepted(){return kind!=Kind.OTHER;}
        @Override public String toString(){return kind.name();}
    }
    static Shape classify(Collection<String> source){
        if(source==null||source.isEmpty()||source.size()>13)return new Shape(Kind.OTHER,0);
        int[] count=new int[13];Set<String> uniques=new HashSet<>();
        for(String c:source){
            if(c==null||c.length()<2||!uniques.add(c))return new Shape(Kind.OTHER,source.size());
            String rank=c.substring(0,c.length()-1);int idx=-1;
            for(int i=0;i<13;i++)if(RANKS[i].equals(rank)){idx=i;break;}
            if(idx<0||"SHDC".indexOf(c.charAt(c.length()-1))<0||++count[idx]>4)
                return new Shape(Kind.OTHER,source.size());
        }
        int n=source.size(),rankKinds=0,low=-1,high=-1;
        for(int i=0;i<13;i++)if(count[i]>0){rankKinds++;if(low<0)low=i;high=i;}
        if(rankKinds==1){
            if(n==1)return new Shape(Kind.SINGLE,n);
            if(n==2)return new Shape(Kind.PAIR,n);
            if(n==3)return new Shape(Kind.TRIPLE,n);
            if(n==4)return new Shape(Kind.FOUR_KIND,n);
        }
        // A 2 cannot belong to a straight or consecutive pairs.
        if(high==12)return new Shape(Kind.OTHER,n);
        if(high-low+1!=rankKinds)return new Shape(Kind.OTHER,n);
        boolean allOne=true,allTwo=true;
        for(int i=low;i<=high;i++){
            if(count[i]!=1)allOne=false;
            if(count[i]!=2)allTwo=false;
        }
        if(n>=3&&allOne)return new Shape(Kind.STRAIGHT,n);
        if(n==6&&allTwo)return new Shape(Kind.THREE_PAIRS,n);
        if(n==8&&allTwo)return new Shape(Kind.FOUR_PAIRS,n);
        return new Shape(Kind.OTHER,n);
    }
    private PlayStructure(){}
}
