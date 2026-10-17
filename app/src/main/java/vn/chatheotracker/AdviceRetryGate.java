package vn.chatheotracker;

/** Retry a suggestion that failed due to incomplete scanning, but only when
 * verified inputs change. Never repeat an identical failing computation. */
final class AdviceRetryGate {
    private String lastInput = "";
    private long lastAttempt = Long.MIN_VALUE/4;
    private boolean blocked = false;
    synchronized void reset(){lastInput="";blocked=false;lastAttempt=Long.MIN_VALUE/4;}
    synchronized void requested(String fingerprint,long nowMs){
        lastInput=fingerprint;lastAttempt=nowMs;blocked=false;
    }
    synchronized void finished(boolean needsData){blocked=needsData;}
    synchronized boolean shouldRetry(String fingerprint,boolean ready,long nowMs){
        return blocked && ready && !lastInput.equals(fingerprint) && nowMs-lastAttempt>=1200;
    }
}
