package vn.chatheotracker;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
public class TestDiagnosticLog {
 public static void main(String[] args)throws Exception{
   File dir=Files.createTempDirectory("diag-check").toFile();
   DiagnosticLog d=new DiagnosticLog(dir);
   d.event("hand_scan","round","cards",Arrays.asList("JD","JH","KS"),"count",3,
       "username","should never be exported","status","stable");
   ByteArrayOutputStream stream=new ByteArrayOutputStream();int n=d.exportTo(stream);
   String s=stream.toString(StandardCharsets.UTF_8.name());
   if(n!=1||!s.contains("JH")||s.contains("username")||s.contains("should never"))throw new AssertionError("diagnostic privacy");
   if(d.bytes()==0)throw new AssertionError("not saved");
   d.clear();if(d.bytes()!=0)throw new AssertionError("not cleared");
   System.out.println("TestDiagnosticLog OK");
 }
}
