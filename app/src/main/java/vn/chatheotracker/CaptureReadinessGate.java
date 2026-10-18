package vn.chatheotracker;

/** Diagnoses whether screen capture is running but has not yet seen Pig Hunters.
 * A decoded frame from the Tracker launcher is not evidence of a broken projection.
 */
final class CaptureReadinessGate {
    enum Status { WAITING, REBIND_ONCE, NEED_GAME_OR_PERMISSION, GAME_VISIBLE }
    private final long createdAt;
    private int frames;
    private boolean sawGame, rebound;
    CaptureReadinessGate(long createdAt){this.createdAt=createdAt;}
    Status observe(boolean hasGameEvidence,long now){
        frames++;
        if(hasGameEvidence){sawGame=true;return Status.GAME_VISIBLE;}
        if(sawGame)return Status.GAME_VISIBLE;
        long elapsed=now-createdAt;
        if(frames>=8 && elapsed>=12000 && !rebound){
            rebound=true;return Status.REBIND_ONCE;
        }
        if(elapsed>=22000 && frames>=8)return Status.NEED_GAME_OR_PERMISSION;
        return Status.WAITING;
    }
    boolean hasSeenGame(){return sawGame;}
}
