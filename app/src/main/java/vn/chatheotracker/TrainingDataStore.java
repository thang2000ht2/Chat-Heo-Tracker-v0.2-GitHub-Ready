package vn.chatheotracker;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Bounded, local, opt-out observational dataset. All entries are JSONL.
 * Only observed cards and outcomes *explicitly entered* by the user are logged.
 * Does not log screenshots, device IDs, nicknames or inferred hidden hands.
 */
final class TrainingDataStore {
    private static final long MAX_BYTES = 4L * 1024 * 1024;
    // MainActivity export and CaptureService capture use different instances.
    private static final Object IO_LOCK = new Object();
    private final File directory;
    TrainingDataStore(File filesDir) {
        directory = new File(filesDir, "ch_context_training");
    }
    boolean append(String event, String roundId, Map<String, ?> attributes) {
        synchronized(IO_LOCK){
        if (!validEvent(event) || !validRoundId(roundId)) return false;
        if (!directory.exists() && !directory.mkdirs()) return false;
        File dest = new File(directory, "observations.jsonl");
        File previous = new File(directory, "observations.previous.jsonl");
        if (dest.length() >= MAX_BYTES) {
            if (previous.exists() && !previous.delete()) return false;
            if (!dest.renameTo(previous)) return false;
        }
        LinkedHashMap<String,Object> record = new LinkedHashMap<>();
        record.put("schema", 1);
        record.put("event", event);
        record.put("round_id", roundId);
        record.put("time_ms", System.currentTimeMillis());
        if (attributes != null) {
            for (Map.Entry<String, ?> entry : attributes.entrySet()) {
                String key = entry.getKey();
                if (key == null || !key.matches("[a-z][a-z0-9_]{0,39}") || record.containsKey(key)) continue;
                record.put(key, entry.getValue());
            }
        }
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(dest, true), StandardCharsets.UTF_8)) {
            writer.write(toJson(record));
            writer.write('\n');
            return true;
        } catch (IOException e) { return false; }
        }
    }
    private static boolean validEvent(String e) {return e != null && e.matches("[a-z_]{2,40}");}
    static boolean validRoundId(String id) {return id != null && id.matches("[0-9a-fA-F-]{36}");}
    static Map<String,Object> fields(Object... pairs) {
        LinkedHashMap<String,Object> map=new LinkedHashMap<>();
        for(int i=0;i+1<pairs.length;i+=2) map.put((String)pairs[i],pairs[i+1]);
        return map;
    }
    static String toJson(Object o) {
        if(o==null)return "null";
        if(o instanceof Boolean || o instanceof Number) {
            if (o instanceof Double && !Double.isFinite((Double)o)) return "null";
            if (o instanceof Float && !Float.isFinite((Float)o)) return "null";
            return o.toString();
        }
        if(o instanceof Map) {
            StringBuilder b=new StringBuilder("{");
            for(Object entryObj:((Map<?,?>)o).entrySet()) {
                Map.Entry<?,?> e=(Map.Entry<?,?>)entryObj;
                if(!(e.getKey() instanceof String))continue;
                if(b.length()>1)b.append(',');
                b.append(toJson(e.getKey())).append(':').append(toJson(e.getValue()));
            }
            return b.append('}').toString();
        }
        if(o instanceof Iterable) {
            StringBuilder b=new StringBuilder("[");
            for(Object v:(Iterable<?>)o){if(b.length()>1)b.append(',');b.append(toJson(v));}
            return b.append(']').toString();
        }
        if(o.getClass().isArray()) {
            StringBuilder b=new StringBuilder("[");
            int size=java.lang.reflect.Array.getLength(o);
            for(int i=0;i<size;i++){if(i>0)b.append(',');b.append(toJson(java.lang.reflect.Array.get(o,i)));}
            return b.append(']').toString();
        }
        String s=String.valueOf(o);
        StringBuilder b=new StringBuilder("\"");
        for(int i=0;i<s.length();i++){
            char c=s.charAt(i);
            switch(c){
                case '"':b.append("\\\"");break;
                case '\\':b.append("\\\\");break;
                case '\n':b.append("\\n");break;
                case '\r':b.append("\\r");break;
                case '\t':b.append("\\t");break;
                default:if(c<32)b.append(String.format(Locale.ROOT,"\\u%04x",(int)c));else b.append(c);
            }
        }
        return b.append('"').toString();
    }
    long bytes(){
        synchronized(IO_LOCK){
        return new File(directory,"observations.jsonl").length()
            +new File(directory,"observations.previous.jsonl").length();
        }
    }
    int exportTo(OutputStream output) throws IOException {
        synchronized(IO_LOCK){
        int lines=0;
        for(String file:new String[]{"observations.previous.jsonl","observations.jsonl"}) {
            File source=new File(directory,file);
            if(!source.isFile())continue;
            try(BufferedReader in=new BufferedReader(new InputStreamReader(new FileInputStream(source),StandardCharsets.UTF_8))) {
                String row;
                while((row=in.readLine())!=null){
                    output.write(row.getBytes(StandardCharsets.UTF_8));output.write('\n');lines++;
                }
            }
        }
        output.flush();
        return lines;
        }
    }
    void clear() {
        synchronized(IO_LOCK){
        new File(directory,"observations.jsonl").delete();
        new File(directory,"observations.previous.jsonl").delete();
        }
    }
}
