package vn.chatheotracker;

import java.util.*;

/** Match an opponent's verified counter decrease to a COMPLETE observed move.
 * The recognizer may expose 8-9-10 first and J-Q a few frames later. Never
 * attribute the first three cards to a five-card drop or silently invent the
 * two missing cards. Pure Java so it can be tested on real capture sequences.
 */
final class PlayBatchMatcher {
    static final class Fragment {
        final List<String> cards;
        final long first, last;
        Fragment(Collection<String> cards,long first,long last){
            this.cards=new ArrayList<>(cards);this.first=first;this.last=last;
        }
    }
    static final class Drop {
        final int seat, delta;
        final long time;
        Drop(int seat,int delta,long time){this.seat=seat;this.delta=delta;this.time=time;}
    }
    static final class Match {
        final int dropIndex;
        final List<Integer> fragments;
        final List<String> cards;
        Match(int d,List<Integer> f,Collection<String> c){
            dropIndex=d;fragments=Collections.unmodifiableList(new ArrayList<>(f));
            cards=Collections.unmodifiableList(new ArrayList<>(c));
        }
    }
    static Match exact(List<Fragment> moves,List<Drop> drops,
                       Collection<String> liveBoard,boolean boardStable,long now){
        if(moves==null||drops==null)return null;
        // Never attribute a card while the active layer is still animating.
        // In an October 18 real capture, 2H was assigned against a visible
        // 2C lead precisely because boardStable was false and the old
        // conditional check was skipped.
        if(!boardStable||liveBoard==null||liveBoard.isEmpty())return null;
        // Ignore invalid counter decreases. A stable WHITE play is positive
        // evidence; it may be a SUBSET of a full play when a card was occluded.
        // Require matched cards to contain all visible white cards, not the
        // other way around. An empty board during animations is inconclusive.
        for(int di=0;di<drops.size();di++){
            Drop d=drops.get(di);
            if(d.delta<1||d.delta>13)continue;
            for(int i=0;i<moves.size();i++){
                Fragment first=moves.get(i);
                if(Math.abs(first.first-d.time)>2600)continue;
                LinkedHashSet<String> union=new LinkedHashSet<>();
                ArrayList<Integer> chosen=new ArrayList<>();
                for(int j=i;j<moves.size()&&j<i+6;j++){
                    Fragment f=moves.get(j);
                    if(Math.abs(f.first-d.time)>2600 || f.first-first.first>2400)break;
                    if(f.cards.isEmpty() || !Collections.disjoint(union,f.cards))break;
                    union.addAll(f.cards);chosen.add(j);
                    if(union.size()>d.delta)break;
                    if(union.size()==d.delta){
                        if(!union.containsAll(liveBoard))continue;
                        return new Match(di,chosen,union);
                    }
                }
            }
        }
        return null;
    }
    private PlayBatchMatcher(){}
}
