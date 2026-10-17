package vn.chatheotracker;

import java.util.*;

/**
 * Local bounded training archive, keyed by the stable visual seat ribbon fingerprint.
 * No screenshots or personal account IDs are saved. Replacing/vacating a seat does
 * not destroy past training samples. This is observational history, not a trained net.
 */
final class OpponentLearningArchive {
    private static final int MAX_IDENTITIES=48;
    private final LinkedHashMap<String,String> profiles=new LinkedHashMap<>();

    void remember(String identity, LearnedOpponentHistory learned) {
        if (!SeatIdentityTracker.valid(identity) || learned==null || learned.size()==0) return;
        profiles.remove(identity);
        profiles.put(identity, learned.encode());
        while (profiles.size()>MAX_IDENTITIES)
            profiles.remove(profiles.keySet().iterator().next());
    }
    LearnedOpponentHistory restore(String identity) {
        String text=SeatIdentityTracker.valid(identity)?profiles.get(identity):null;
        return LearnedOpponentHistory.decode(text);
    }
    int profiles(){return profiles.size();}
    int recordedEntries(){
        int sum=0;
        for(String v:profiles.values())sum+=LearnedOpponentHistory.decode(v).size();
        return sum;
    }
    void clear(){profiles.clear();}
    String encode(){
        StringBuilder result=new StringBuilder("archive1\n");
        for(Map.Entry<String,String> e:profiles.entrySet())
            result.append(e.getKey()).append('\t').append(e.getValue()).append('\n');
        return result.toString();
    }
    static OpponentLearningArchive decode(String raw){
        OpponentLearningArchive archive=new OpponentLearningArchive();
        if(raw==null || raw.length()>600000 || !raw.startsWith("archive1\n"))return archive;
        for(String line:raw.substring(9).split("\n")){
            int sep=line.indexOf('\t');
            if(sep<=0)continue;
            String identity=line.substring(0,sep);
            if(!SeatIdentityTracker.valid(identity))continue;
            LearnedOpponentHistory history=LearnedOpponentHistory.decode(line.substring(sep+1));
            archive.remember(identity,history);
        }
        return archive;
    }
}
