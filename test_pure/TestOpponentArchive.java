package vn.chatheotracker;
import java.util.*;
public final class TestOpponentArchive {
    static void ok(boolean v, String reason){if(!v)throw new AssertionError(reason);}
    static String id(int step){
        char[] b=new char[SeatIdentityTracker.BITS];Arrays.fill(b,'0');
        for(int i=0;i<300;i++)if((i+step)%4==0)b[i]='1';
        return new String(b);
    }
    public static void main(String[] args){
        String first=id(0),other=id(1);
        LearnedOpponentHistory a=new LearnedOpponentHistory();
        a.record(Arrays.asList("9S","9H"));a.newRound();a.record(Arrays.asList("JS"));
        OpponentLearningArchive archive=new OpponentLearningArchive();
        archive.remember(first,a);
        ok(archive.profiles()==1,"old opponent preserved");
        ok(archive.restore(first).size()==3,"moves and round boundary preserved");
        ok(archive.restore(other).size()==0,"new occupant starts without old strategy");
        OpponentLearningArchive reload=OpponentLearningArchive.decode(archive.encode());
        ok(reload.restore(first).snapshot().get(0).equals(Arrays.asList("9S","9H")),"archive survives restart");
        reload.clear();ok(reload.profiles()==0,"explicit wipe works");
        System.out.println("TestOpponentArchive OK");
    }
}
