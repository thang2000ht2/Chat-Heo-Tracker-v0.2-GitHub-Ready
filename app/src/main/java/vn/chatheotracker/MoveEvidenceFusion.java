package vn.chatheotracker;

import java.util.*;

/** Evidence fusion for ANY visible legal move (singles, sets, straights, 3/4
 * consecutive pairs). A verified count decrease is ONE piece of evidence,
 * never the sole determinant of card identity. Gray historical layers must
 * NEVER enter observe(). No unseen card is synthesized.
 *
 * Deliberately abstains if there are competing actors or incompatible sets.
 */
final class MoveEvidenceFusion {
    static final class CounterChange {
        final int seat,delta;final long when;
        CounterChange(int seat,int delta,long when){this.seat=seat;this.delta=delta;this.when=when;}
    }
    static final class Observation {
        final Set<String> cards;final long when;
        Observation(Collection<String> cards,long when){
            this.cards=Collections.unmodifiableSet(new LinkedHashSet<>(cards));this.when=when;
        }
    }
    static final class Outcome {
        final int dropIndex,seat,delta,observations;
        final List<String> cards;
        final String shape,reason;
        Outcome(int index,CounterChange drop,Collection<String> cards,String shape,
                int observations,String reason){
            this.dropIndex=index;this.seat=drop.seat;this.delta=drop.delta;
            this.cards=Collections.unmodifiableList(new ArrayList<>(cards));
            this.shape=shape;this.observations=observations;this.reason=reason;
        }
    }
    private static final long HISTORY_MS=5200L;
    private static final long LEAD_WINDOW_MS=3200L;
    private static final long OBS_WINDOW_MS=3500L;
    private final ArrayDeque<Observation> whiteFrames=new ArrayDeque<>();
    private long lastWhite=-1;

    void observe(Collection<String> activeWhite,long now){
        if(lastWhite>0 && now-lastWhite>1400)whiteFrames.clear();
        if(activeWhite!=null&&!activeWhite.isEmpty()){
            // Duplicate labels on one frame are not independent evidence.
            whiteFrames.addLast(new Observation(activeWhite,now));
            lastWhite=now;
        }
        while(!whiteFrames.isEmpty() && now-whiteFrames.peekFirst().when>HISTORY_MS)
            whiteFrames.removeFirst();
    }
    /** Grace period for counter OCR lag after the bright play has vanished. */
    boolean hasRecentActive(long now){return lastWhite>=0 && now-lastWhite<=1100;}
    void clear(){whiteFrames.clear();lastWhite=-1;}

    Outcome match(List<CounterChange> drops, Collection<String> played,
                  Collection<String> myHand, Collection<String> currentWhite,long now){
        if(drops==null || drops.isEmpty() || whiteFrames.isEmpty())return null;
        Set<String> forbidden=new HashSet<>();
        if(played!=null)forbidden.addAll(played);
        if(myHand!=null)forbidden.addAll(myHand);
        LinkedHashSet<String> live=new LinkedHashSet<>();
        if(currentWhite!=null)live.addAll(currentWhite);
        ArrayList<Proposal> candidates=new ArrayList<>();
        for(int i=0;i<drops.size();i++){
            CounterChange d=drops.get(i);
            if(d.seat<0||d.seat>2||d.delta<1||d.delta>13||Math.abs(now-d.when)>LEAD_WINDOW_MS)continue;
            // Competing counter changes near the same move make attribution
            // uncertain, even if one is not the right number of cards.
            boolean competing=false;
            for(int j=0;j<drops.size();j++){
                if(i!=j && drops.get(j).seat!=d.seat &&
                        Math.abs(d.when-drops.get(j).when)<1000){competing=true;break;}
            }
            if(competing)continue;
            ArrayList<Observation> near=new ArrayList<>();
            for(Observation f:whiteFrames)
                if(Math.abs(f.when-d.when)<=OBS_WINDOW_MS &&
                   now-f.when<=HISTORY_MS && f.cards.size()<=d.delta &&
                   Collections.disjoint(f.cards,forbidden))near.add(f);
            if(near.isEmpty())continue;
            // Full sightings are reliable even if seen only once, provided
            // counter drop matches and the active board is consistent.
            for(Observation f:near){
                if(f.cards.size()!=d.delta)continue;
                addProposal(candidates,i,d,f.cards,near,near,live,forbidden,3);
            }
            // Partial sightings can expose different parts of a fan across
            // frames. Search contiguous windows, rather than assuming an
            // entire move must be visible in one frame.
            for(int start=0;start<near.size();start++){
                LinkedHashSet<String> union=new LinkedHashSet<>();
                for(int end=start;end<near.size()&&end<start+14;end++){
                    if(near.get(end).when-near.get(start).when>2200)break;
                    union.addAll(near.get(end).cards);
                    if(union.size()>d.delta)break;
                    if(union.size()==d.delta && end>start){
                        addProposal(candidates,i,d,union,
                            near.subList(start,end+1),near,live,forbidden,2);
                    }
                }
            }
        }
        if(candidates.isEmpty())return null;
        // Prefer repeated corroboration, but do NOT choose arbitrarily when
        // competing full legal reconstructions survive the same evidence.
        candidates.sort((a,b)->Integer.compare(b.score,a.score));
        Proposal best=candidates.get(0);
        for(Proposal p:candidates){
            if(p.dropIndex!=best.dropIndex && p.score>=best.score-1)return null;
            if(p.dropIndex==best.dropIndex && !p.cards.equals(best.cards) &&
                    p.score>=best.score-1)return null;
        }
        return new Outcome(best.dropIndex,drops.get(best.dropIndex),best.cards,
            best.shape,best.support,best.origin);
    }
    private static final class Proposal {
        final int dropIndex,score,support;final Set<String> cards;
        final String shape,origin;
        Proposal(int dropIndex,Set<String> cards,String shape,int score,int support,String origin){
            this.dropIndex=dropIndex;this.cards=new LinkedHashSet<>(cards);
            this.shape=shape;this.score=score;this.support=support;this.origin=origin;
        }
    }
    private static void addProposal(List<Proposal> result,int index,CounterChange drop,
                                    Collection<String> cards,List<Observation> support,
                                    List<Observation> neighborhood,Set<String> live,Set<String> forbidden,int base){
        LinkedHashSet<String> unique=new LinkedHashSet<>(cards);
        if(unique.size()!=drop.delta || !Collections.disjoint(unique,forbidden))return;
        if(!live.isEmpty() && !unique.containsAll(live))return;
        // A different active white play observed at the same counter event
        // invalidates this reconstruction, even if its cards are a subset of
        // another earlier candidate. Prefer missing data over false history.
        for(Observation f:neighborhood)
            if(Math.abs(f.when-drop.when)<=850 && !unique.containsAll(f.cards))return;
        PlayStructure.Shape shape=PlayStructure.classify(unique);
        if(!shape.accepted())return;
        int matchingFrames=0;
        for(Observation f:support)if(unique.containsAll(f.cards))matchingFrames++;
        int score=base+Math.min(3,matchingFrames);
        if(!live.isEmpty() && live.size()==drop.delta && live.equals(unique))score+=2;
        result.add(new Proposal(index,unique,shape.kind.name(),score,matchingFrames,
            base==3?"complete_active_layer":"multiple_partial_active_layers"));
    }
}
