package vn.chatheotracker;
import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
public final class TestTrainingDataStore {
    public static void main(String[] args) throws Exception {
        File dir=Files.createTempDirectory("tracker-data").toFile();
        TrainingDataStore s=new TrainingDataStore(dir);
        String id=UUID.randomUUID().toString();
        if(s.append("opponent_move",id,TrainingDataStore.fields("cards",Arrays.asList("3S","3H"),"confidence","counter_matched"))!=true)throw new AssertionError();
        s.append("round_result",id,TrainingDataStore.fields("placement",2));
        ByteArrayOutputStream output=new ByteArrayOutputStream();
        if(s.exportTo(output)!=2)throw new AssertionError("not two observations");
        String[] lines=output.toString("UTF-8").trim().split("\\n");
        if(!lines[0].contains("\"opponent_move\"")||!lines[1].contains("\"placement\":2"))throw new AssertionError();
        if(s.append("bad event",id,null))throw new AssertionError("bad event permitted");
        if(!TrainingDataStore.toJson("a\"\\\n").equals("\"a\\\"\\\\\\n\""))throw new AssertionError("escape failed");
        s.clear();
        if(s.bytes()!=0)throw new AssertionError("not deleted");
        System.out.println("Training dataset event/export/erase PASS");
    }
}
