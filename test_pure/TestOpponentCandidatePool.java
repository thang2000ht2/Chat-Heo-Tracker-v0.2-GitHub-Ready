package vn.chatheotracker;
import java.util.*;
/** Verifies colors and candidate runs are derived from cards NOT held by us. */
public final class TestOpponentCandidatePool {
    static void ok(boolean b,String message){if(!b)throw new AssertionError(message);}
    static List<String> deck(){
        List<String> cards=new ArrayList<>();
        for(String rank:CardWarnings.RANKS)for(String suit:new String[]{"S","C","D","H"})cards.add(rank+suit);
        return cards;
    }
    public static void main(String[] args){
        List<String> full=deck();
        OpponentCandidatePool.Result unknown=OpponentCandidatePool.analyze(full,Arrays.asList("8S"),false);
        ok(!unknown.reliable,"Unverified hand must not create confident warnings");
        ok(unknown.cue("8S")==OpponentCandidatePool.Cue.MINE,"Even when deck mismatch prevents warnings, a visually verified held card must stay dim");
        ok(!unknown.quartets.isEmpty(),"Provisional quartet highlights stay visible during scanning");
        ok(unknown.cue("9C")==OpponentCandidatePool.Cue.POSSIBLE_QUARTET,
            "Unverified hand still highlights structurally possible quartets");
        ok(unknown.compactCounts().startsWith("dự kiến?"),
            "Provisional colors are clearly labeled as unconfirmed");
        ok(!unknown.quartets.contains("8"),
            "A recognized held eight excludes quartet 8 even during scanning");
        OpponentCandidatePool.Result one=OpponentCandidatePool.analyze(full,Arrays.asList("8S"),true);
        ok(one.reliable,"Verified hand");
        ok(!one.possible.contains("8S") && one.possible.size()==51,"Subtract my 8S");
        ok(!one.quartets.contains("8"),"A known 8S rules out all opponent quartets of eights");
        ok(one.cue("8S")==OpponentCandidatePool.Cue.MINE,"Owned suit must be neutral-gray, not red or green");
        ok(one.cue("8C")!=OpponentCandidatePool.Cue.POSSIBLE_QUARTET,"Other eights also stop showing quartet red");
        ok(one.cue("9C")==OpponentCandidatePool.Cue.POSSIBLE_QUARTET,"A complete possible quartet is red");
        OpponentCandidatePool.Result late3=OpponentCandidatePool.analyze(full,Arrays.asList("8S"),true,new int[]{3,3,3});
        ok(late3.quartets.isEmpty() && late3.threePairs.isEmpty() && late3.fourPairs.isEmpty(),
            "Nobody with at most 3 cards can have a quartet or 6/8-card pair run");
        ok(late3.cue("9C")==OpponentCandidatePool.Cue.NEUTRAL,"No impossible red highlight at 3 cards");
        OpponentCandidatePool.Result late5=OpponentCandidatePool.analyze(full,Arrays.asList("8S"),true,new int[]{5,4,5});
        ok(!late5.quartets.isEmpty() && late5.threePairs.isEmpty() && late5.fourPairs.isEmpty(),
            "5-card seats can have quartets, not 3/4 pair runs");
        OpponentCandidatePool.Result late7=OpponentCandidatePool.analyze(full,Arrays.asList("8S"),true,new int[]{7,7,6});
        ok(!late7.threePairs.isEmpty() && late7.fourPairs.isEmpty(),
            "7-card seats can hold 3 pairs, never 4 pairs");
        OpponentCandidatePool.Result pendingLate=OpponentCandidatePool.analyze(full,
            Arrays.asList("8S"),false,new int[]{5,4,5});
        ok(pendingLate.cue("9C")==OpponentCandidatePool.Cue.POSSIBLE_QUARTET,
            "Pending scan still shows possible quartet color");
        ok(pendingLate.threePairs.isEmpty() && pendingLate.fourPairs.isEmpty(),
            "Pending scan cannot invent 6/8-card combinations in five-card hands");
        ok(pendingLate.cue("8S")==OpponentCandidatePool.Cue.MINE,
            "Recognized own card still stays dim in provisional mode");
        ok(one.quartets.contains("9"),"Do not eliminate other ranks");
        ok(one.warnings.remainingByRank[5]==3,"Only 3 eights remain for opponents");
        List<String> publicCards=Arrays.asList("9S","9D","4H");
        List<String> remain=new ArrayList<>(full);remain.removeAll(publicCards);
        OpponentCandidatePool.Result seen=OpponentCandidatePool.analyze(remain,Arrays.asList("8S"),true);
        ok(!seen.quartets.contains("9"),"Two public nines eliminate quartet possibility");
        ok(!seen.possible.contains("9S") && !seen.possible.contains("4H"),"Public cards eliminated");

        List<String> paired=Arrays.asList("3S","3H","4S","4H","5S","5H","6S","6H");
        OpponentCandidatePool.Result runs=OpponentCandidatePool.analyze(paired,Arrays.asList("3S","6H"),true);
        ok(!runs.warnings.hasTriple && !runs.warnings.hasQuad,"Holding a card at each end breaks 3/4 consecutive pairs");
        ok(runs.cue("3S")==OpponentCandidatePool.Cue.MINE,"A held card at one end never receives green");
        ok(runs.cue("4S")==OpponentCandidatePool.Cue.NEUTRAL,"A broken run does not receive green");
        ok(runs.threePairs.isEmpty() && runs.fourPairs.isEmpty(),"No candidate sequences remain");
        OpponentCandidatePool.Result all=OpponentCandidatePool.analyze(paired,Collections.singleton("2S"),true);
        ok(!all.reliable,"A verified hand that is not in the unplayed pool is stale");
        OpponentCandidatePool.Result none=OpponentCandidatePool.analyze(full,Collections.emptyList(),true);
        ok(!none.reliable,"Unknown local hand must not be marked reliable");
        ok(none.cue("9C")==OpponentCandidatePool.Cue.POSSIBLE_QUARTET,
            "Unknown hand: visible tentative colors, not silently hidden");
        OpponentCandidatePool.Result mismatch=OpponentCandidatePool.analyze(remain,Arrays.asList("9S"),true);
        ok(!mismatch.reliable,"A public card cannot also be in my verified hand");
        ok(mismatch.cue("3S")==OpponentCandidatePool.Cue.POSSIBLE_QUARTET,
            "A historical conflict should not hide all otherwise possible colors");
        ok(mismatch.compactCounts().contains("dự kiến?"),"Conflicting history is labeled tentative");

        // Holding 3 suits at four spaced ranks blocks every 3/4-pair run.
        List<String> ours=Arrays.asList("3S","3C","3D","6S","6C","6D", "9S","9C","9D","QS","QC","QD","2S");
        OpponentCandidatePool.Result noRuns=OpponentCandidatePool.analyze(full,ours,true);
        ok(noRuns.threePairs.isEmpty() && noRuns.fourPairs.isEmpty(),"Every consecutive triplet hits 3,6,9 or Q bottleneck");
        List<List<List<String>>> history=Arrays.asList(Collections.emptyList(),Collections.emptyList(),Collections.emptyList());
        ThreatScenarioSampler.Seat[] estimates=ThreatScenarioSampler.sample(ours,Collections.emptyList(),new int[]{13,13,13},history,1234);
        for(ThreatScenarioSampler.Seat seat:estimates){
            for(int i=0;i<2;i++)ok(seat.hits[i]==0 && seat.percent(i)==0,"Sampler must never resurrect a ruled-out pair run");
        }
        // Holding one card of every rank makes ANY quartet impossible.
        List<String> onePerRank=new ArrayList<>();
        for(String rank:CardWarnings.RANKS)onePerRank.add(rank+"S");
        OpponentCandidatePool.Result noQuartets=OpponentCandidatePool.analyze(full,onePerRank,true);
        ok(noQuartets.quartets.isEmpty(),"One known card of every rank rules out every opponent quartet");
        ThreatScenarioSampler.Seat[] q=ThreatScenarioSampler.sample(onePerRank,Collections.emptyList(),new int[]{13,13,13},history,1234);
        for(ThreatScenarioSampler.Seat seat:q)ok(seat.hits[2]==0 && seat.percent(2)==0,"Sampler must never resurrect a ruled-out quartet");
        System.out.println("OpponentCandidatePool OK");
    }
}
