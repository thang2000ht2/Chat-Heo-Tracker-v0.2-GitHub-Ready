package vn.chatheotracker;

/**
 * Offline ranking objective; this is an expected-tempo proxy, NOT an actual
 * probability of finishing 1st / 2nd / 3rd / 4th. Lower is preferred.
 */
final class FinishPriority {
    static double cost(int estimatedFutureTurns, int cardsAfter, int cardsPlayed,
                       double responseRisk, double chopRisk, int fewestOpponentCards,
                       StrategyMode mode){
        // In placement mode avoid hoarding cards when an opponent is very close to finishing.
        boolean placement=mode==StrategyMode.PLACEMENT;
        double urgency=(fewestOpponentCards<=2?0.19:fewestOpponentCards<=4?0.10:0.025);
        double controlPenalty=placement?0.90:1.42;
        double chopPenalty=placement?1.35:2.05;
        return estimatedFutureTurns
             + controlPenalty*clamp(responseRisk)
             + chopPenalty*clamp(chopRisk)
             + (placement?urgency:urgency*.45)*Math.max(0,cardsAfter)
             - (placement?0.11:0.025)*Math.max(0,cardsPlayed);
    }
    private static double clamp(double x){
        if(Double.isNaN(x)||Double.isInfinite(x))return 1;
        return Math.max(0,Math.min(1,x));
    }
    private FinishPriority(){}
}
