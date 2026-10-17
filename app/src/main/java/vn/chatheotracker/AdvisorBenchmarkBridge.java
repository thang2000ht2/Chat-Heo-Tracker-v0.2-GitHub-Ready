package vn.chatheotracker;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Research only. Runs the REAL APK advisor from stdin; never imports Python policy heuristics. */
public final class AdvisorBenchmarkBridge {
    private static List<String> csv(String v) {
        if (v==null||v.isEmpty()||v.equals("-")) return Collections.emptyList();
        return Arrays.asList(v.split(","));
    }
    public static void main(String[] args) throws Exception {
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in,StandardCharsets.UTF_8));
        PrintWriter output = new PrintWriter(new OutputStreamWriter(System.out,StandardCharsets.UTF_8),true);
        String line;
        while ((line=input.readLine())!=null) {
            if (line.equals("EXIT")) break;
            try {
                String[] parts=line.split("\\|",-1);
                if(parts.length!=4)throw new IllegalArgumentException("expected 4 fields");
                List<String> hand=csv(parts[0]),table=csv(parts[1]),seen=csv(parts[2]);
                String[] tokens=parts[3].split(",");
                if(tokens.length!=3)throw new IllegalArgumentException("three opponent counts");
                int[] counts={Integer.parseInt(tokens[0]),Integer.parseInt(tokens[1]),Integer.parseInt(tokens[2])};
                MonteCarloAdvisor.Advice advice=MonteCarloAdvisor.adviseBehaviorAware(
                    hand,table,seen,counts,Collections.emptyList(),StrategyMode.PLACEMENT);
                output.println(advice.title.replace('\n',' '));
            }catch(Exception error){output.println("ERROR " + error.getClass().getSimpleName());}
        }
    }
}
