package vn.chatheotracker;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Evidence across time for partially obscured card layouts. No hidden-card guessing. */
final class TemporalCardEvidence {
    private static final long GAP_MS=1450L;
    private static final long TTL_MS=2200L;
    private static final class Entry {
        int hits=1;
        long lastSeen;
        Entry(long now){lastSeen=now;}
    }
    private final Map<String,Entry> candidates=new LinkedHashMap<>();

    LinkedHashSet<String> update(Collection<String> visible,Set<String> alreadyPlayed,
                                 Set<String> waiting,long now){
        LinkedHashSet<String> result=new LinkedHashSet<>();
        for(String card:new HashSet<>(visible)){
            if(alreadyPlayed.contains(card)||waiting.contains(card)){
                candidates.remove(card);
                continue;
            }
            Entry ev=candidates.get(card);
            if(ev==null||now-ev.lastSeen>GAP_MS){
                ev=new Entry(now);
                candidates.put(card,ev);
            }else ev.hits++;
            ev.lastSeen=now;
            if(ev.hits>=2){result.add(card);candidates.remove(card);}
        }
        Iterator<Map.Entry<String,Entry>> it=candidates.entrySet().iterator();
        while(it.hasNext()){
            if(now-it.next().getValue().lastSeen>TTL_MS)it.remove();
        }
        return result;
    }
    void clear(){candidates.clear();}
}
