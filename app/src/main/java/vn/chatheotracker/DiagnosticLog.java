package vn.chatheotracker;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Offline, bounded diagnostic log. Never writes frames, nicknames, account
 * identifiers or filesystem paths. The caller supplies ONLY recognized card
 * tokens, counters, reason codes and workflow state. Separate from training.
 */
final class DiagnosticLog {
    private static final Object LOCK=new Object();
    private static final long LIMIT=1_500_000L;
    private final File directory;
    DiagnosticLog(File files){directory=new File(files,"ch_tracker_diagnostics");}
    void event(String kind,String round,Object... fields){
        if(kind==null||!kind.matches("[a-z_]{2,40}"))return;
        synchronized(LOCK){
            if(!directory.exists()&&!directory.mkdirs())return;
            File active=new File(directory,"diagnostic.jsonl");
            if(active.length()>LIMIT){
                File old=new File(directory,"diagnostic.previous.jsonl");
                if(old.exists()&&!old.delete())return;
                if(!active.renameTo(old))return;
            }
            Map<String,Object> row=new LinkedHashMap<>();
            row.put("schema",1);row.put("time_ms",System.currentTimeMillis());
            row.put("event",kind);row.put("round_id",round==null?"unknown":round);
            for(int i=0;i+1<fields.length;i+=2){
                if(!(fields[i] instanceof String))continue;
                String key=(String)fields[i];
                if(!key.matches("[a-z][a-z0-9_]{0,39}")||key.equals("event")||key.equals("round_id"))continue;
                // No arbitrary strings: this whitelist is the privacy boundary.
                if(!ALLOWED.contains(key))continue;
                row.put(key,fields[i+1]);
            }
            try(Writer w=new OutputStreamWriter(new FileOutputStream(active,true),StandardCharsets.UTF_8)){
                w.write(TrainingDataStore.toJson(row));w.write('\n');
            }catch(IOException ignored){}
        }
    }
    private static final Set<String> ALLOWED=new HashSet<>(Arrays.asList(
        "cards","slots","seen","board","hand","counts","deltas","seat","delta",
        "count","before","after","fan_count","recognized","complete","diagnosis",
        "reason","status","confidence","fragments","missing","source","verified",
        "epoch","mismatch","exception_type","duration_ms","frame_index"));
    int exportTo(OutputStream out)throws IOException{
        synchronized(LOCK){
            int lines=0;
            for(String f:new String[]{"diagnostic.previous.jsonl","diagnostic.jsonl"}){
                File src=new File(directory,f);if(!src.isFile())continue;
                try(BufferedReader in=new BufferedReader(new InputStreamReader(new FileInputStream(src),StandardCharsets.UTF_8))){
                    String s;while((s=in.readLine())!=null){out.write(s.getBytes(StandardCharsets.UTF_8));out.write('\n');lines++;}
                }
            }
            out.flush();return lines;
        }
    }
    long bytes(){synchronized(LOCK){return new File(directory,"diagnostic.jsonl").length()
      +new File(directory,"diagnostic.previous.jsonl").length();}}
    void clear(){synchronized(LOCK){new File(directory,"diagnostic.jsonl").delete();
      new File(directory,"diagnostic.previous.jsonl").delete();}}
}
