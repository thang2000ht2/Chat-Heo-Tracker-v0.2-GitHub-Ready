package vn.chatheotracker;

import java.util.*;

/** Only show a chopping warning for an actually recommended play containing a 2.
 * Simulated rates are heuristic and depend on the unverified house rules. */
final class HeoWarningAdvisor {
    static String warningForMove(Collection<String> proposed, Collection<String> lead, double chopRisk) {
        if(proposed==null || proposed.isEmpty() || Double.isNaN(chopRisk) ||
           Double.isInfinite(chopRisk) || chopRisk<=0) return "";
        boolean playingTwo=false;
        for(String card:proposed)if(MonteCarloAdvisor.rank(card)==12)playingTwo=true;
        if(!playingTwo)return "";
        // A lone heo does not answer a pair. Reject mismatched families even
        // when an upstream caller accidentally attempts to show a warning.
        if(lead!=null && !lead.isEmpty() && lead.size()!=proposed.size())return "";
        // Only a candidate structural threat is known, never a calibrated %. 
        return "⚠ HEO: có khả năng có hàng chặt (chưa định lượng)";
    }
    private HeoWarningAdvisor(){}
}
