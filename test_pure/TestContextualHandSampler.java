package vn.chatheotracker;

import java.util.*;

public final class TestContextualHandSampler {
    private static void check(boolean x,String msg){if(!x)throw new AssertionError(msg);}
    private static List<List<String>> history(String ...moves){
        List<List<String>> out=new ArrayList<>();
        for(String m:moves)out.add(Arrays.asList(m.split(" +")));
        return out;
    }
    public static void main(String[] args){
        ArrayList<String> deck=new ArrayList<>();
        for(String rank:MonteCarloAdvisor.RANKS)
            for(String suit:MonteCarloAdvisor.SUITS)deck.add(rank+suit);
        ArrayList<String> mine=new ArrayList<>(deck.subList(0,13));
        ArrayList<String> hidden=new ArrayList<>(deck.subList(13,52));
        int[] counts={13,13,13};
        OpponentBehaviorModel.Profile neutral=new OpponentBehaviorModel.Profile(Collections.emptyList());
        OpponentBehaviorModel.Profile pairStyle=new OpponentBehaviorModel.Profile(
            history("7S 7C","8H 8D","9H 9D","JS JC","3S 3D"));
        OpponentBehaviorModel.Profile singleStyle=new OpponentBehaviorModel.Profile(
            history("3S","4S","5S","6S","7S"));
        int[] empty=new int[13];empty[MonteCarloAdvisor.rank("KH")]=1;
        double pairFactor=ContextualHandSampler.contextMultiplier(empty,MonteCarloAdvisor.rank("KC"),pairStyle);
        double soloFactor=ContextualHandSampler.contextMultiplier(empty,MonteCarloAdvisor.rank("KC"),singleStyle);
        check(pairFactor>soloFactor,"Grouped-play style should mildly weight sampled pairs");
        check(ContextualHandSampler.contextMultiplier(empty,10,neutral)==1,"Unknown behavior remains unweighted");
        Random rng=new Random(15);
        for(int trial=0;trial<1000;trial++){
            List<List<String>> out=ContextualHandSampler.draw(hidden,counts,
                new OpponentBehaviorModel.Profile[]{pairStyle,singleStyle,neutral},rng);
            HashSet<String> all=new HashSet<>();
            for(int p=0;p<3;p++){
                check(out.get(p).size()==counts[p],"Exact player count in trial "+trial);
                for(String s:out.get(p))check(all.add(s),"No duplicate cards");
            }
            check(all.equals(new HashSet<>(hidden)),"All hidden cards assigned exactly once");
        }
        boolean failed=false;
        try{ContextualHandSampler.draw(hidden,new int[]{12,13,13},null,rng);}
        catch(IllegalArgumentException expected){failed=true;}
        check(failed,"Reject inconsistent 52-card accounting");
        // Deterministic data -> reproducible recommendation (same RNG seed).
        ArrayList<String> manual=new ArrayList<>(mine);
        Collections.shuffle(manual,new Random(4));
        MonteCarloAdvisor.Advice a=MonteCarloAdvisor.adviseBehaviorAware(manual,Collections.emptyList(),
            Collections.emptyList(),counts,null,StrategyMode.PLACEMENT);
        MonteCarloAdvisor.Advice b=MonteCarloAdvisor.adviseBehaviorAware(manual,Collections.emptyList(),
            Collections.emptyList(),counts,null,StrategyMode.PLACEMENT);
        check(a.title.equals(b.title)&&a.detail.equals(b.detail),"Offline advice reproducible");
        check(a.title.startsWith("ĐÁNH "),"Gives one concrete recommendation");
        check(a.detail.contains("chưa hiệu chuẩn") || a.detail.contains("mô phỏng"),"Clearly marks model estimate");
        System.out.println("TestContextualHandSampler OK (1000 legal hidden deals; deterministic suggestion)");
    }
}
