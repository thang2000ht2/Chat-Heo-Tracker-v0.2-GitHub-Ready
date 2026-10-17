package vn.chatheotracker;
import java.util.*;
public class TestThreatPassSensitivity {
 static void yes(boolean ok,String msg){if(!ok)throw new AssertionError(msg);}
 public static void main(String[] args){
  List<String> hand=new ArrayList<>();
  for(String r:MonteCarloAdvisor.RANKS)hand.add(r+"H");
  List<List<List<String>>> history=Arrays.asList(Collections.emptyList(),Collections.emptyList(),Collections.emptyList());
  int[] sizes={13,13,13};
  ThreatScenarioSampler.Seat[] baseline=ThreatScenarioSampler.sample(hand,Collections.emptyList(),sizes,history,123);
  List<List<ObservedPass>> events=new ArrayList<>();
  for(int p=0;p<3;p++)events.add(new ArrayList<>());
  for(int i=0;i<4;i++)events.get(0).add(new ObservedPass(Arrays.asList("2H"),13));
  ThreatScenarioSampler.Seat[] after=ThreatScenarioSampler.sample(hand,Collections.emptyList(),sizes,history,events,123);
  yes(after[0].weights[2]<=baseline[0].weights[2]+0.01 || after[0].percent(2)<=baseline[0].percent(2),"Pass evidence should not boost chopped world probability");
  yes(after[1].percent(0)==baseline[1].percent(0),"Seat 1 should not change from seat 0 pass evidence");
  System.out.println("TestThreatPassSensitivity OK before="+baseline[0].concise()+" after="+after[0].concise());
 }
}
