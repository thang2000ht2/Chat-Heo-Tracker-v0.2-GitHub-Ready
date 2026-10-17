package vn.chatheotracker;

import java.util.*;

/** Cards that may be in opponents' hands after removing our verified hand and
 * all public cards. A possible combination in this pool is NOT proof that any
 * single opponent holds it. No guessed/occluded card is silently marked used.
 */
final class OpponentCandidatePool {
    enum Cue { UNVERIFIED, MINE, NEUTRAL, POSSIBLE_3_PAIRS, POSSIBLE_4_PAIRS, POSSIBLE_QUARTET }
    static final class Result {
        final boolean reliable;
        final Set<String> possible;
        final Set<String> mine;
        final CardWarnings.Result warnings;
        final List<String> quartets, threePairs, fourPairs;
        Result(boolean reliable, Set<String> possible, Set<String> mine,
               CardWarnings.Result warnings, List<String> quartets,
               List<String> threePairs, List<String> fourPairs) {
            this.reliable=reliable;
            this.possible=Collections.unmodifiableSet(possible);
            this.mine=Collections.unmodifiableSet(mine);
            this.warnings=warnings;
            this.quartets=Collections.unmodifiableList(quartets);
            this.threePairs=Collections.unmodifiableList(threePairs);
            this.fourPairs=Collections.unmodifiableList(fourPairs);
        }
        /** Card-specific cue so owned cards are never painted as an opponent threat. */
        Cue cue(String card) {
            if(!reliable)return Cue.UNVERIFIED;
            if(mine.contains(card))return Cue.MINE;
            if(!possible.contains(card))return Cue.NEUTRAL;
            int rank=MonteCarloAdvisor.rank(card);
            if(rank<0)return Cue.NEUTRAL;
            if(quartets.contains(CardWarnings.RANKS[rank]))return Cue.POSSIBLE_QUARTET;
            if(warnings.inQuad[rank])return Cue.POSSIBLE_4_PAIRS;
            if(warnings.inTriple[rank])return Cue.POSSIBLE_3_PAIRS;
            return Cue.NEUTRAL;
        }
        String compactCounts() {
            if(!reliable)return "cần xác nhận bài tay";
            // Counts are structural possibilities in the shared pool, NOT the
            // number of combinations in any one opponent's actual hand.
            return "ngoài tay " + possible.size() + " · TQ:" + quartets.size()
                + " 3ĐT:" + threePairs.size() + " 4ĐT:" + fourPairs.size();
        }
    }

    static Result analyze(Collection<String> unplayed, Collection<String> confirmedHand,
                          boolean handVerified) {
        return analyze(unplayed,confirmedHand,handVerified,null);
    }
    /** Opponent counts also rule out groups too large for ANY single seat.
     * Missing/unrecognized counters never create false impossibilities.
     */
    static Result analyze(Collection<String> unplayed, Collection<String> confirmedHand,
                          boolean handVerified,int[] opponentCounts) {
        Set<String> left=new HashSet<>(),mine=new HashSet<>();
        boolean valid=unplayed!=null && confirmedHand!=null && handVerified && !confirmedHand.isEmpty();
        if(unplayed!=null)for(String c:unplayed){
            if(!RecentMoves.isCard(c) || !left.add(c))valid=false;
        }
        if(confirmedHand!=null)for(String c:confirmedHand){
            if(!RecentMoves.isCard(c) || !mine.add(c))valid=false;
        }
        if(!left.containsAll(mine))valid=false;
        if(!valid)return new Result(false,new HashSet<>(),mine,
            CardWarnings.analyze(Collections.emptyList()),
            new ArrayList<>(),new ArrayList<>(),new ArrayList<>());
        left.removeAll(mine);
        CardWarnings.Result warnings=CardWarnings.analyze(left);
        int maxCards=13;
        if(opponentCounts!=null && opponentCounts.length==3){
            boolean validCounters=true;int largest=0;
            for(int count:opponentCounts){
                if(count<0||count>13){validCounters=false;break;}
                largest=Math.max(largest,count);
            }
            if(validCounters)maxCards=largest;
        }
        if(maxCards<6){warnings.hasTriple=false;Arrays.fill(warnings.inTriple,false);}
        if(maxCards<8){warnings.hasQuad=false;Arrays.fill(warnings.inQuad,false);}
        List<String> quartets=new ArrayList<>(),three=new ArrayList<>(),four=new ArrayList<>();
        String[] ranks=CardWarnings.RANKS;
        if(maxCards>=4)for(int r=0;r<ranks.length;r++)
            if(warnings.remainingByRank[r]==4)quartets.add(ranks[r]);
        if(maxCards>=6)for(int start=0;start<=9;start++) {
            if(hasPairs(warnings,start,3))three.add(ranks[start]+"–"+ranks[start+2]);
        }
        if(maxCards>=8)for(int start=0;start<=8;start++) {
            if(hasPairs(warnings,start,4))four.add(ranks[start]+"–"+ranks[start+3]);
        }
        // Cues derive from the feasible combinations above, never from raw
        // rank multiplicity when the relevant opponent hand is too short.
        return new Result(true,left,mine,warnings,quartets,three,four);
    }
    private static boolean hasPairs(CardWarnings.Result result,int start,int n){
        for(int j=0;j<n;j++)if(result.remainingByRank[start+j]<2)return false;
        return true;
    }
    private OpponentCandidatePool(){}
}
