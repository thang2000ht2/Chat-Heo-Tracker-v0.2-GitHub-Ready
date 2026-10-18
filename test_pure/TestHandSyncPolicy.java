package vn.chatheotracker;
public class TestHandSyncPolicy {
 public static void main(String[] a){
   if(HandSyncPolicy.acceptFirstVisualHand(8,false,new int[]{13,13,13}))throw new AssertionError("partial initial deal");
   if(!HandSyncPolicy.acceptFirstVisualHand(8,false,new int[]{9,6,6}))throw new AssertionError("late entry must synchronize 8 observed cards");
   if(!HandSyncPolicy.acceptFirstVisualHand(3,true,new int[]{-1,5,0}))throw new AssertionError("late 3-card hand");
   if(!HandSyncPolicy.acceptFirstVisualHand(13,false,new int[]{-1,-1,-1}))throw new AssertionError("new deal 13");
   System.out.println("TestHandSyncPolicy OK");
 }
}
