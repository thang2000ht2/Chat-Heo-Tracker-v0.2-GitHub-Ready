package vn.chatheotracker;

import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.*;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.*;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.BackgroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.util.TypedValue;
import android.view.*;
import android.widget.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Local-only capture & overlay service. Does not connect to game process or network. */
public final class CaptureService extends Service {
    static final String ACTION_START="vn.chatheotracker.START",ACTION_STOP="vn.chatheotracker.STOP";
    static final String ACTION_RESET="vn.chatheotracker.RESET";
    static final String ACTION_RESCAN="vn.chatheotracker.RESCAN";
    static final String ACTION_CLEAR_LEARNING="vn.chatheotracker.CLEAR_LEARNING";
    static final String ACTION_SET_HAND="vn.chatheotracker.SET_HAND";
    static final String EXTRA_HAND_TEXT="hand_text";
    static final String ACTION_ADD="vn.chatheotracker.ADD", ACTION_REMOVE="vn.chatheotracker.REMOVE";
    static final String ACTION_NEXT_PLAYER="vn.chatheotracker.NEXT_PLAYER", ACTION_SET_PLAYER="vn.chatheotracker.SET_PLAYER";
    static final String EXTRA_RESULT="projection_result",EXTRA_DATA="projection_data",EXTRA_CARD="card",EXTRA_PLAYER="player";
    private MediaProjection projection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;
    private HandlerThread worker;
    private Handler workerHandler, mainHandler;
    private WindowManager wm;
    // Independent overlays placed near each player and just above the local hand.
    private final View[] panels=new View[4];
    private final WindowManager.LayoutParams[] panelParams=new WindowManager.LayoutParams[4];
    private final TextView[] playerRows=new TextView[3];
    private TextView handRow, otherRow, remainingRow;
    private TextView scanBadge;
    private WindowManager.LayoutParams scanBadgeParams;
    private volatile String scanDiagnosis="CHƯA QUÉT";
    private volatile int scanVisibleCards=0;
    private long lastScanBadgeUpdate=0;
    // Only this small top-left window accepts touches. Card HUD windows are pass-through.
    private TextView resetButton, suggestButton, suggestionText;
    private WindowManager.LayoutParams resetButtonParams, suggestButtonParams, suggestionPanelParams;
    private View suggestionPanel;
    private volatile long lastFrameArrived=0, lastFrameDecoded=0;
    private long lastRecoveryAttempt=0;
    private int scanFrameIndex=0;
    private final Runnable captureWatchdog=new Runnable(){
        @Override public void run(){
            if(stopping || projection==null || workerHandler==null)return;
            long now=SystemClock.elapsedRealtime();
            boolean streamStalled=lastFrameArrived>0 && now-lastFrameArrived>7000;
            boolean decodeStalled=lastFrameDecoded>0 && now-lastFrameDecoded>7000;
            if(streamStalled || decodeStalled)setScanStatus("mất khung hình");
            if((streamStalled || decodeStalled) && now-lastRecoveryAttempt>15000){
                lastRecoveryAttempt=now;
                workerHandler.post(()->rebindCaptureSurface());
            }
            mainHandler.postDelayed(this,3000);
        }
    };
    private Runnable dismissSuggestion;
    private CardRecognizer recognizer;
    private CountRecognizer countRecognizer;
    private HandRecognizer handRecognizer;
    private final TemporalHandEvidence fusedHand=new TemporalHandEvidence();
    private final LinkedHashSet<String> handCards=new LinkedHashSet<>();
    private final LinkedHashSet<String> myPlayed=new LinkedHashSet<>();
    private String handPending="";
    private int handPendingFrames=0;
    private boolean manualHandOverride=false;
    // Fresh screenshot observations, never stale manual table text.
    private List<String> currentBoard=Collections.emptyList();
    private String boardCandidate="";
    private int boardFrames=0;
    private boolean boardStable=false;
    private boolean myTurn=false;
    private int turnFrames=0;
    private final ExecutorService adviceExecutor=Executors.newSingleThreadExecutor();
    private boolean autoAdvicePending=false;
    private int autoAdviceScanFrames=0;
    private int absentTurnFrames=0;
    private int turnEpoch=0;
    private int adviceRequestId=0;
    private boolean suggestRequested=false;
    private int suggestionScanFrames=0;
    private int lastVisualHandSize=-1;
    private boolean visualHandTrusted=false;
    private String visualCandidate="";
    private int visualFrames=0;


    private final Object stateLock=new Object();
    private long lastScanned=0;
    // Each card is confirmed independently: an icon/chat box can temporarily hide
    // only part of a played combination, so whole-frame signatures are brittle.
    private final TemporalCardEvidence recentCards=new TemporalCardEvidence();
    private final LinkedHashSet<String> played=new LinkedHashSet<>();
    private final LinkedHashSet<String>[] byPlayer=new LinkedHashSet[]{new LinkedHashSet<>(),new LinkedHashSet<>(),new LinkedHashSet<>()};
    private final LinkedHashSet<String> otherPlayed=new LinkedHashSet<>();
    // Play history is newest-first and grouped by actual observed move, not card rank.
    private final RecentMoves[] playerHistory={new RecentMoves(),new RecentMoves(),new RecentMoves()};
    // On-device learned play patterns belong to the current occupant of each seat.
    // These persist across hands, unlike the public cards of the current hand.
    private final LearnedOpponentHistory[] learning={new LearnedOpponentHistory(),new LearnedOpponentHistory(),new LearnedOpponentHistory()};
    private OpponentLearningArchive trainingArchive=new OpponentLearningArchive();
    private final SeatIdentityTracker[] seatIdentity=new SeatIdentityTracker[3];
    private final RecentMoves otherHistory=new RecentMoves();
    private final LinkedHashSet<String> waitingCards=new LinkedHashSet<>();
    private final ArrayList<CardMove> cardMoves=new ArrayList<>();
    private final ArrayList<CountDrop> countDrops=new ArrayList<>();
    private final int[] knownCounts={-1,-1,-1},lastCandidates={-1,-1,-1},candidateFrames={0,0,0};
    private final int[] missingCards={0,0,0};
    private boolean sawAnyLowerCounter=false;
    private int newDealStableFrames=0;
    private long ignoreCardsUntil=0L;
    private boolean stopping=false;
    private int densityDpi;

    @Override public void onCreate(){super.onCreate();mainHandler=new Handler(Looper.getMainLooper());}
    @Override public int onStartCommand(Intent intent,int flags,int startId){
        if(intent==null)return START_NOT_STICKY;
        String action=intent.getAction();
        if(ACTION_STOP.equals(action)){stopSelf();return START_NOT_STICKY;}
        if(ACTION_RESET.equals(action)){
            manualReset();
            return START_NOT_STICKY;
        }
        if(ACTION_RESCAN.equals(action)){
            forceRescan();
            return START_NOT_STICKY;
        }
        if(ACTION_CLEAR_LEARNING.equals(action)){
            synchronized(stateLock){
                for(int i=0;i<3;i++)learning[i].clear();
                trainingArchive.clear();
                getSharedPreferences("opponent_learning_v1",MODE_PRIVATE).edit()
                    .remove("moves_0").remove("moves_1").remove("moves_2")
                    .remove("training_archive").apply();
                adviceRequestId++;
            }
            if(projection==null)stopSelf();
            return START_NOT_STICKY;
        }
        if(ACTION_SET_HAND.equals(action)){
            StrategyAdvisor.Input candidate=StrategyAdvisor.parse(intent.getStringExtra(EXTRA_HAND_TEXT));
            if(candidate.error==null && !candidate.cards.isEmpty()){
                synchronized(stateLock){
                    handCards.clear();handCards.addAll(candidate.cards);
                    manualHandOverride=true;
                    handPending="";handPendingFrames=0;
                    visualHandTrusted=false;visualFrames=0;
                    persistCurrentHand();
                    getSharedPreferences("tracker_strategy",MODE_PRIVATE).edit()
                        .putString("hand_source","manual").apply();
                    adviceRequestId++;
                }
                refresh();
            }
            if(projection==null)stopSelf();
            return START_NOT_STICKY;
        }
        if(ACTION_ADD.equals(action)||ACTION_REMOVE.equals(action)){
            String c=intent.getStringExtra(EXTRA_CARD);
            if(c!=null&&!c.isEmpty())synchronized(stateLock){
                if(ACTION_ADD.equals(action)){
                    if(played.add(c)){
                        otherPlayed.add(c);
                        otherHistory.record(Collections.singletonList(c));
                    }
                }else{
                    played.remove(c);otherPlayed.remove(c);waitingCards.remove(c);
                    for(LinkedHashSet<String> set:byPlayer)set.remove(c);
                    for(RecentMoves h:playerHistory)h.remove(c);
                    otherHistory.remove(c);
                }
            }
            refresh();return START_NOT_STICKY;
        }
        if(ACTION_START.equals(action) && projection==null){
            try{
                beginNotification();
                int code=intent.getIntExtra(EXTRA_RESULT,Activity.RESULT_CANCELED);
                Intent data;
                if(Build.VERSION.SDK_INT>=33)data=intent.getParcelableExtra(EXTRA_DATA,Intent.class);
                else data=intent.getParcelableExtra(EXTRA_DATA);
                if(code!=Activity.RESULT_OK || data==null){stopSelf();return START_NOT_STICKY;}
                startScreenCapture(code,data);
            }catch(Exception err){android.util.Log.e("Tracker","Start error",err);stopSelf();}
        }
        return START_NOT_STICKY;
    }
    private void beginNotification(){
        NotificationManager nm=getSystemService(NotificationManager.class);
        NotificationChannel channel=new NotificationChannel("tracker","Theo dõi bài",NotificationManager.IMPORTANCE_LOW);
        nm.createNotificationChannel(channel);
        Intent stop=new Intent(this,CaptureService.class).setAction(ACTION_STOP);
        PendingIntent pi=PendingIntent.getService(this,4,stop,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification n=new Notification.Builder(this,"tracker")
            .setContentTitle("Chặt Heo Tracker đang chạy")
            .setContentText("Đang đọc hình ảnh màn hình theo quyền bạn đã cấp")
            .setSmallIcon(android.R.drawable.ic_menu_view).setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel,"Dừng",pi).build();
        if(Build.VERSION.SDK_INT>=29)startForeground(77,n,ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
        else startForeground(77,n);
    }
    private void startScreenCapture(int code,Intent data){
        recognizer=new CardRecognizer(this);
        // Restore explicitly edited hand only. Previous automatic scans may be stale.
        android.content.SharedPreferences pref=getSharedPreferences("tracker_strategy",MODE_PRIVATE);
        String savedSource=pref.getString("hand_source", "");
        if("manual".equals(savedSource)){
            StrategyAdvisor.Input previously=StrategyAdvisor.parse(pref.getString("hand",""));
            if(previously.error==null){
                synchronized(stateLock){handCards.addAll(previously.cards);manualHandOverride=true;}
            }
        }else pref.edit().remove("hand").remove("hand_source").apply();
        countRecognizer=new CountRecognizer(this);
        loadOpponentLearning();
        handRecognizer=new HandRecognizer(this);
        wm=(WindowManager)getSystemService(Context.WINDOW_SERVICE);
        createPanel();
        worker=new HandlerThread("CardScan");worker.start();workerHandler=new Handler(worker.getLooper());
        MediaProjectionManager pm=(MediaProjectionManager)getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        projection=pm.getMediaProjection(code,data);
        projection.registerCallback(new MediaProjection.Callback(){
            @Override public void onStop(){mainHandler.post(()->stopSelf());}
            @Override public void onCapturedContentResize(int width,int height) {
                mainHandler.post(() -> {
                    resizeCapture(width,height);
                    relayoutOverlays(width,height);
                });
            }
        },mainHandler);
        android.util.DisplayMetrics dm=new android.util.DisplayMetrics();
        wm.getDefaultDisplay().getRealMetrics(dm);
        densityDpi=dm.densityDpi;
        int width=dm.widthPixels,height=dm.heightPixels;
        imageReader=ImageReader.newInstance(width,height,PixelFormat.RGBA_8888,2);
        imageReader.setOnImageAvailableListener(this::onImage,workerHandler);
        virtualDisplay=projection.createVirtualDisplay("LocalCardCapture",width,height,dm.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,imageReader.getSurface(),null,workerHandler);
        lastFrameArrived=SystemClock.elapsedRealtime();
        mainHandler.postDelayed(captureWatchdog,3000);
    }
    private void resizeCapture(int width,int height){
        if(virtualDisplay==null || width<=0 || height<=0)return;
        if(imageReader!=null && imageReader.getWidth()==width && imageReader.getHeight()==height)return;
        try {
            ImageReader old=imageReader;
            ImageReader next=ImageReader.newInstance(width,height,PixelFormat.RGBA_8888,2);
            next.setOnImageAvailableListener(this::onImage,workerHandler);
            imageReader=next;
            virtualDisplay.resize(width,height,densityDpi);
            virtualDisplay.setSurface(next.getSurface());
            if(old!=null){old.setOnImageAvailableListener(null,null);old.close();}
        }catch(Exception e){android.util.Log.e("Tracker","Resize capture failed",e);}
    }
    /** Reconnect the existing virtual display surface (not a second MediaProjection).
     * Android 14+ forbids starting another capture session with the same consent token.
     */
    private void rebindCaptureSurface(){
        if(stopping || projection==null || virtualDisplay==null || imageReader==null)return;
        try{
            ImageReader old=imageReader;
            ImageReader next=ImageReader.newInstance(old.getWidth(),old.getHeight(),PixelFormat.RGBA_8888,2);
            next.setOnImageAvailableListener(this::onImage,workerHandler);
            virtualDisplay.setSurface(next.getSurface());
            imageReader=next;
            old.setOnImageAvailableListener(null,null);
            old.close();
            recentCards.clear();
            lastScanned=0;
            setScanStatus("nối lại luồng hình");
            android.util.Log.i("Tracker","Reconnected stalled image surface");
        }catch(Exception e){
            android.util.Log.e("Tracker","Capture reconnection failed; new screen permission may be needed",e);
        }
    }
    private void onImage(ImageReader reader){
        Image image=null;
        try {
            image=reader.acquireLatestImage();
            if(image==null)return;
            long now=SystemClock.elapsedRealtime();
            lastFrameArrived=now;
            if(now-lastScanned<210)return;
            scanFrameIndex++;
            lastScanned=now;
            Image.Plane plane=image.getPlanes()[0];
            int w=image.getWidth(),h=image.getHeight();
            int pixelStride=plane.getPixelStride(),rowStride=plane.getRowStride();
            int rowPadding=rowStride-pixelStride*w;
            Bitmap full=Bitmap.createBitmap(w+rowPadding/pixelStride,h,Bitmap.Config.ARGB_8888);
            ByteBuffer buffer=plane.getBuffer();buffer.rewind();full.copyPixelsFromBuffer(buffer);
            Bitmap cropped=Bitmap.createBitmap(full,0,0,w,h);
            full.recycle();
            Bitmap normalized=Bitmap.createScaledBitmap(cropped,2048,942,false);
            if(normalized!=cropped)cropped.recycle();
            List<String> cards=recognizer.recognize(normalized);
            List<String> whiteCards=recognizer.recognizeCurrentPlay(normalized);
            int[] counts=countRecognizer.read(normalized);
            boolean turnNow=LocalTurnDetector.isMyTurn(normalized);
            // Nicknames change rarely. Reading them every frame adds delay and OCR noise.
            boolean readSeats=scanFrameIndex%8==0;
            String[] seatNames=readSeats?SeatVisualSignature.read(normalized):null;
            // Scan the user's fan more frequently when absent, inconsistent, or the
            // player is about to act; otherwise run it every second frame.
            boolean readHand=handCards.isEmpty() || autoAdvicePending || suggestRequested
                || scanFrameIndex%2==0;
            HandRecognizer.Result myCards=null;
            if(readHand){
                // Full and partial fans now share one recognizer. Calling both used
                // to repeat costly image processing on every unsuccessful frame.
                HandRecognizer.Result raw=handRecognizer.recognizeVisibleHand(normalized);
                // Integrate corners that were visible in different frames rather
                // than rejecting a full hand when chat obscures one frame.
                List<String> fused=fusedHand.accept(raw.slots,raw.fanCount,raw.fanStart);
                myCards=fused.isEmpty()?raw:new HandRecognizer.Result(fused,true,"GHÉP NHIỀU KHUNG");
            }
            normalized.recycle();
            lastFrameDecoded=SystemClock.elapsedRealtime();
            // Recognition and reconciliation are separate checks. Readable card glyphs
            // must not be mistaken for a verified hand if the deck totals disagree.
            boolean changed,trySuggestion=false,turnBegan=false,turnEnded=false;
            synchronized(stateLock){
                boolean occupantChanged=readSeats && handleSeatIdentities(seatNames,counts);
                changed=handleFrame(cards,counts,now) || occupantChanged;
                if(myCards!=null){
                    if(handleHandFrame(myCards))changed=true;
                    if(updateVisibleHand(myCards))changed=true;
                }
                updateBoard(whiteCards);
                if(turnNow){
                    turnFrames++;absentTurnFrames=0;
                    if(!myTurn && turnFrames>=2){
                        myTurn=true;turnEpoch++;
                        autoAdvicePending=true;autoAdviceScanFrames=0;
                        turnBegan=true;
                    }
                }else{
                    turnFrames=0;
                    if(myTurn && ++absentTurnFrames>=3){
                        myTurn=false;turnEpoch++;autoAdvicePending=false;
                        suggestRequested=false;adviceRequestId++;turnEnded=true;
                    }
                }
                if(myTurn && autoAdvicePending){
                    autoAdviceScanFrames++;
                    // At least three post-turn frames for screen / card history stabilization.
                    // If counters/hand still mismatch, keep scanning for up to ~3 seconds.
                    PartialDeckState.Result deck=PartialDeckState.check(
                        handCards,observedCards(),knownCounts);
                    boolean ready=boardStable && !handCards.isEmpty() &&
                        deck.feasible && deck.missing<=24 &&
                        (visualHandTrusted||manualHandOverride||!handPending.isEmpty());
                    if((ready&&autoAdviceScanFrames>=2)||autoAdviceScanFrames>=22){
                        autoAdvicePending=false;trySuggestion=true;
                    }
                }
                if(suggestRequested){
                    suggestionScanFrames++;
                    if(suggestionScanFrames>=5){
                        suggestRequested=false;autoAdvicePending=false;trySuggestion=true;
                    }
                }
            }
            if(now-lastScanBadgeUpdate>1400){
                lastScanBadgeUpdate=now;
                String state;
                synchronized(stateLock){
                    if(!handCards.isEmpty()){
                        PartialDeckState.Result check=PartialDeckState.check(
                            handCards,observedCards(),knownCounts);
                        if(check.exact)state=handCards.size()+" lá ✓";
                        else if(check.feasible)
                            state=handCards.size()+" lá · thiếu lịch sử "+check.missing;
                        else state=handCards.size()+" lá · chưa đối soát";
                    }else if(myCards!=null){
                        state=myCards.complete?(myCards.cards.size()+" lá · chưa đồng bộ"):
                            (myCards.cards.size()+" lá · "+myCards.diagnosis);
                    }else state="đã nhận hình";
                }
                setScanStatus(state);
            }
            if(changed)mainHandler.post(this::refresh);
            if(turnBegan){
                final String warning;
                synchronized(stateLock){warning=HeoWarningAdvisor.warning(handCards,observedCards());}
                mainHandler.post(()->{
                    showPendingAdvice("ĐANG QUÉT BÀI…");
                    if(!warning.isEmpty())showUrgentHeoWarning(warning);
                });
            }
            if(turnEnded)mainHandler.post(()->{
                if(dismissSuggestion!=null)mainHandler.removeCallbacks(dismissSuggestion);
                if(suggestionPanel!=null)suggestionPanel.setVisibility(View.GONE);
                if(panels[3]!=null)panels[3].setVisibility(View.VISIBLE);
            });
            if(trySuggestion)runFreshAdvice();
        }catch(Exception e){
            android.util.Log.w("Tracker","Frame scan error",e);
            setScanStatus("lỗi khung hình");
        }
        finally {if(image!=null)image.close();}
    }
    /** Accept only a complete, stable, 13-card fan; never count partial guesses as a hand.
     * A manual correction takes precedence until the next reset/deal.
     */
    private boolean handleHandFrame(HandRecognizer.Result result){
        if(!result.complete || manualHandOverride || !handCards.isEmpty()){
            handPending="";handPendingFrames=0;return false;
        }
        // If cards have been played already, seeing 13 may be an unrelated transition:
        // only acquire a fresh hand before the local player has played anything.
        if(!myPlayed.isEmpty())return false;
        LinkedHashSet<String> stable=new LinkedHashSet<>(result.cards);
        if(stable.size()!=13)return false;
        for(String c:stable)if(played.contains(c))return false;
        ArrayList<String> sorted=new ArrayList<>(stable);
        sorted.sort((a,b)->{
            int r=MonteCarloAdvisor.rank(a)-MonteCarloAdvisor.rank(b);
            return r!=0?r:MonteCarloAdvisor.suit(a)-MonteCarloAdvisor.suit(b);
        });
        String signature=android.text.TextUtils.join(" ",sorted);
        if(signature.equals(handPending))handPendingFrames++;
        else{handPending=signature;handPendingFrames=1;}
        if(handPendingFrames<3)return false;
        handCards.addAll(sorted);
        getSharedPreferences("tracker_strategy",MODE_PRIVATE).edit().putString("hand",signature).putString("hand_source","auto").apply();
        return true;
    }
    private void updateHandAfterPlay(Collection<String> cards){
        if(handCards.isEmpty())return;
        boolean changed=false;
        for(String c:cards){
            if(handCards.remove(c)){
                myPlayed.add(c);
                otherPlayed.remove(c);otherHistory.remove(c);
                changed=true;
            }
        }
        if(changed)persistCurrentHand();
    }
    private void persistCurrentHand(){
        // Keep an empty value at the end of a hand. Never regenerate cards after they are spent.
        getSharedPreferences("tracker_strategy",MODE_PRIVATE).edit()
            .putString("hand",android.text.TextUtils.join(" ",handCards)).apply();
    }
    /** On the worker thread. A counter drop, not turn rotation, identifies the actor. */
    private boolean handleFrame(List<String> visible,int[] counts,long now){
        boolean changed=false;
        for(int i=0;i<3;i++){
            int count=counts[i];
            if(count<0||count>13){lastCandidates[i]=-1;candidateFrames[i]=0;continue;}
            // Winner counters can disappear at 0. Never interpret startup,
            // covered badges or a missing low-quality OCR digit as a turn.
            if(count==0 && (knownCounts[i]<1 || knownCounts[i]>3))continue;
            if(lastCandidates[i]==count)candidateFrames[i]++;
            else {lastCandidates[i]=count;candidateFrames[i]=1;}
            if(candidateFrames[i]<2 || knownCounts[i]==count)continue;
            int before=knownCounts[i];
            knownCounts[i]=count;
            if(before>count){
                countDrops.add(new CountDrop(i,before-count,now));
                sawAnyLowerCounter=true;
                changed=true;
            }else if(before>=0 && count==13){changed=true;}
        }
        // A redeal is recognized only after actual progress in the previous hand.
        // Require all three freshly READ counters to be 13 for five successive
        // sampled frames (~1.5s) to reject transition animations / OCR glitches.
        boolean completeDeal=sawAnyLowerCounter && allThirteen();
        if(completeDeal){
            for(int i=0;i<3;i++){
                if(counts[i]!=13 || lastCandidates[i]!=13 || candidateFrames[i]<2){
                    completeDeal=false;break;
                }
            }
        }
        if(completeDeal){
            newDealStableFrames++;
            if(newDealStableFrames>=5){
                resetRoundInternal();
                for(int i=0;i<3;i++){
                    knownCounts[i]=13;lastCandidates[i]=13;candidateFrames[i]=2;
                }
                // Ignore residual table cards while the new deal animates.
                ignoreCardsUntil=now+1400;
                return true;
            }
        }else newDealStableFrames=0;
        if(now<ignoreCardsUntil){
            recentCards.clear();
            return changed;
        }
        // A card may disappear for a few frames behind chat/emotes. Accumulate
        // per-card evidence over time instead of demanding identical full lists.
        LinkedHashSet<String> newlySeen=recentCards.update(visible,played,waitingCards,now);
        if(!newlySeen.isEmpty()){
            waitingCards.addAll(newlySeen);
            // Merge cards exposed at different instants in the same animation.
            if(!cardMoves.isEmpty() && now-cardMoves.get(cardMoves.size()-1).updated<750){
                CardMove last=cardMoves.get(cardMoves.size()-1);
                last.cards.addAll(newlySeen);
                last.updated=now;
            }else cardMoves.add(new CardMove(newlySeen,now));
            changed=true;
        }
        if(reconcile(now))changed=true;
        return changed;
    }
    private boolean allThirteen(){
        for(int value:knownCounts)if(value!=13)return false;
        return true;
    }
    private boolean reconcile(long now){
        boolean changed=false;
        // Prefer same-number-of-cards drop events within ~2 seconds of the visible play.
        for(int m=0;m<cardMoves.size();){
            CardMove move=cardMoves.get(m);
            int selected=-1,bestScore=Integer.MAX_VALUE;
            for(int d=0;d<countDrops.size();d++){
                CountDrop drop=countDrops.get(d);
                long lag=Math.abs(drop.time-move.time);
                if(lag>1900)continue;
                int score=(int)lag + Math.abs(drop.delta-move.cards.size())*300;
                if(score<bestScore){bestScore=score;selected=d;}
            }
            if(selected>=0 && (countDrops.get(selected).delta==move.cards.size()
                    || (now-move.time>=1000 && now-move.updated>=650))){
                CountDrop drop=countDrops.remove(selected);
                ArrayList<String> accepted=new ArrayList<>();
                for(String card:move.cards)if(played.add(card)){
                    byPlayer[drop.player].add(card);
                    accepted.add(card);
                }
                playerHistory[drop.player].record(accepted);
                if(!accepted.isEmpty()){
                    learning[drop.player].record(accepted);
                    persistOpponentLearning(drop.player);
                }
                if(drop.delta>move.cards.size())missingCards[drop.player]+=drop.delta-move.cards.size();
                waitingCards.removeAll(move.cards);
                cardMoves.remove(m);
                changed=true;
            }else if(now-move.time>2600){
                // This could be the user's own turn; without a visible count change,
                // the app must NOT invent an opponent attribution.
                ArrayList<String> accepted=new ArrayList<>();
                for(String card:move.cards)if(played.add(card)){
                    otherPlayed.add(card);
                    accepted.add(card);
                }
                otherHistory.record(accepted);
                updateHandAfterPlay(accepted);
                waitingCards.removeAll(move.cards);
                cardMoves.remove(m);changed=true;
            }else m++;
        }
        for(int i=0;i<countDrops.size();){
            CountDrop drop=countDrops.get(i);
            if(now-drop.time>4000){
                missingCards[drop.player]+=drop.delta;
                countDrops.remove(i);changed=true;
            }else i++;
        }
        return changed;
    }
    /** Restore active profiles and the long-term archive; replacement never deletes evidence. */
    private void loadOpponentLearning(){
        android.content.SharedPreferences sp=getSharedPreferences("opponent_learning_v1",MODE_PRIVATE);
        trainingArchive=OpponentLearningArchive.decode(sp.getString("training_archive", ""));
        for(int i=0;i<3;i++){
            learning[i]=LearnedOpponentHistory.decode(sp.getString("moves_"+i,""));
            seatIdentity[i]=new SeatIdentityTracker(sp.getString("fingerprint_"+i,null));
            String id=seatIdentity[i].active();
            if(id!=null && learning[i].size()>0)trainingArchive.remember(id,learning[i]);
            else if(id==null && learning[i].size()>0){
                // Legacy seat history without a verified fingerprint: keep it in a
                // separate legacy slot instead of attributing it to a new person.
                sp.edit().putString("legacy_moves_"+i,learning[i].encode()).apply();
                learning[i]=new LearnedOpponentHistory();
            }
        }
        persistArchive();
    }
    private void persistArchive(){
        getSharedPreferences("opponent_learning_v1",MODE_PRIVATE).edit()
            .putString("training_archive",trainingArchive.encode()).apply();
    }
    private void persistOpponentLearning(int i){
        if(i<0||i>=3)return;
        if(seatIdentity[i]!=null && seatIdentity[i].active()!=null){
            trainingArchive.remember(seatIdentity[i].active(),learning[i]);
        }
        getSharedPreferences("opponent_learning_v1",MODE_PRIVATE).edit()
            .putString("moves_"+i,learning[i].encode())
            .putString("training_archive",trainingArchive.encode()).apply();
    }
    private void persistIdentity(int i){
        android.content.SharedPreferences.Editor e=
            getSharedPreferences("opponent_learning_v1",MODE_PRIVATE).edit();
        if(seatIdentity[i].active()==null)e.remove("fingerprint_"+i);
        else e.putString("fingerprint_"+i,seatIdentity[i].active());
        e.putString("moves_"+i,learning[i].encode())
            .putString("training_archive",trainingArchive.encode()).apply();
    }
    /** Distinguish occupants without ever deleting their historical training record. */
    private boolean handleSeatIdentities(String[] signatures,int[] counts){
        if(seatIdentity[0]==null)return false;
        boolean changed=false;
        for(int i=0;i<3;i++){
            boolean roomVisible=false;
            for(int j=0;j<3;j++)if(j!=i && signatures[j]!=null &&
                    counts!=null && j<counts.length && counts[j]>=0)roomVisible=true;
            String oldIdentity=seatIdentity[i].active();
            SeatIdentityTracker.Event ev=seatIdentity[i].observe(signatures[i],roomVisible);
            if(ev==SeatIdentityTracker.Event.NONE)continue;
            if(ev==SeatIdentityTracker.Event.REPLACED || ev==SeatIdentityTracker.Event.VACATED){
                trainingArchive.remember(oldIdentity,learning[i]);
                // Reset only the CURRENT seat session. Older opponents remain in archive.
                learning[i]=new LearnedOpponentHistory();
                if(ev==SeatIdentityTracker.Event.REPLACED)
                    learning[i]=trainingArchive.restore(seatIdentity[i].active());
                // Do not erase already seen moves from the current deal when a
                // player leaves: they are part of the same 52-card deck. The
                // next confirmed deal will clear these visual round rows.
                knownCounts[i]=-1;lastCandidates[i]=-1;candidateFrames[i]=0;
                for(int k=countDrops.size()-1;k>=0;k--)if(countDrops.get(k).player==i)countDrops.remove(k);
                adviceRequestId++;autoAdvicePending=myTurn;
                autoAdviceScanFrames=0;
                changed=true;
            }else if(ev==SeatIdentityTracker.Event.FIRST_SEEN){
                // Restore a previously seen player if they returned to this seat.
                trainingArchive.remember(oldIdentity,learning[i]);
                learning[i]=trainingArchive.restore(seatIdentity[i].active());
                changed=true;
            }
            persistIdentity(i);
        }
        return changed;
    }

    private void resetRoundInternal(){
        for(int i=0;i<3;i++){
            learning[i].newRound();
            persistOpponentLearning(i);
        }
        played.clear();otherPlayed.clear();waitingCards.clear();cardMoves.clear();countDrops.clear();
        handCards.clear();myPlayed.clear();handPending="";handPendingFrames=0;
        fusedHand.clear();
        manualHandOverride=false;
        suggestRequested=false;suggestionScanFrames=0;
        autoAdvicePending=false;autoAdviceScanFrames=0;
        adviceRequestId++;turnEpoch++;absentTurnFrames=0;
        boardStable=false;boardCandidate="";boardFrames=0;currentBoard=Collections.emptyList();
        myTurn=false;turnFrames=0;
        visualHandTrusted=false;visualCandidate="";visualFrames=0;lastVisualHandSize=-1;
        for(LinkedHashSet<String> cards:byPlayer)cards.clear();
        for(RecentMoves history:playerHistory)history.clear();
        otherHistory.clear();
        Arrays.fill(knownCounts,-1);Arrays.fill(lastCandidates,-1);
        Arrays.fill(candidateFrames,0);Arrays.fill(missingCards,0);
        recentCards.clear();sawAnyLowerCounter=false;
        newDealStableFrames=0;
        getSharedPreferences("tracker_strategy",MODE_PRIVATE).edit().remove("hand").remove("table").remove("hand_source").apply();
        mainHandler.post(()->{
            if(suggestionPanel!=null)suggestionPanel.setVisibility(View.GONE);
            if(panels[3]!=null)panels[3].setVisibility(View.VISIBLE);
        });
    }
    private void manualReset(){
        setScanStatus("reset · quét mới");
        synchronized(stateLock){
            resetRoundInternal();
            ignoreCardsUntil=SystemClock.elapsedRealtime()+250;
            lastScanned=0;
        }
        if(projection==null || virtualDisplay==null){
            Toast.makeText(this,"Ghi màn hình đã dừng. Mở Tracker và cấp lại quyền.",Toast.LENGTH_LONG).show();
        }else if(lastFrameArrived>0 && SystemClock.elapsedRealtime()-lastFrameArrived>4500){
            if(workerHandler!=null)workerHandler.post(this::rebindCaptureSurface);
        }
        refresh();
    }
    /** Recovery without deleting already observed cards and training records. */
    private void forceRescan(){
        setScanStatus("quét lại");
        synchronized(stateLock){
            recentCards.clear();
            handPending="";handPendingFrames=0;fusedHand.clear();
            visualCandidate="";visualFrames=0;visualHandTrusted=false;
            boardCandidate="";boardFrames=0;boardStable=false;
            lastScanned=0;
            Arrays.fill(lastCandidates,-1);Arrays.fill(candidateFrames,0);
            adviceRequestId++;
            if(myTurn){autoAdvicePending=true;autoAdviceScanFrames=0;}
        }
        if(projection==null || virtualDisplay==null){
            Toast.makeText(this,"Quyền ghi màn hình đã dừng. Hãy cấp lại trong Tracker.",Toast.LENGTH_LONG).show();
            if(projection==null)stopSelf();
        }else if(workerHandler!=null){
            workerHandler.post(this::rebindCaptureSurface);
        }
        refresh();
    }
    private static final class CardMove {
        final LinkedHashSet<String> cards;
        final long time;
        long updated;
        CardMove(Collection<String> found,long timestamp){cards=new LinkedHashSet<>(found);time=timestamp;updated=timestamp;}
    }
    private static final class CountDrop {
        final int player,delta;final long time;
        CountDrop(int id,int size,long timestamp){player=id;delta=size;time=timestamp;}
    }
    private int dp(float x){return (int)(x*getResources().getDisplayMetrics().density+0.5f);}

    /** All card lists wrap naturally; no horizontal swiping, fixed single line, or maxLines. */
    private TextView newCardText(float sizeSp){
        TextView view=new TextView(this);
        view.setTextSize(sizeSp);
        view.setTextColor(0xffffffff);
        view.setSingleLine(false);
        view.setHorizontallyScrolling(false);
        view.setEllipsize(null);
        view.setBreakStrategy(android.text.Layout.BREAK_STRATEGY_HIGH_QUALITY);
        view.setLineSpacing(0f,1.03f);
        view.setIncludeFontPadding(false);
        view.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        view.setShadowLayer(1.7f,1f,1.2f,0xff030609);
        return view;
    }
    private LinearLayout newPanel(){
        LinearLayout layout=new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(6),dp(4),dp(6),dp(4));
        android.graphics.drawable.GradientDrawable background=new android.graphics.drawable.GradientDrawable();
        background.setColor(0xf0191d2a);
        background.setCornerRadius(dp(6));
        background.setStroke(Math.max(1,dp(.5f)),0x99949ba8);
        layout.setBackground(background);
        return layout;
    }
    private TextView newCaption(String label){
        TextView t=new TextView(this);
        t.setText(label);t.setTextSize(10);t.setTextColor(0xffffdfa1);
        t.setIncludeFontPadding(false);
        return t;
    }
    private void createPanel(){
        if(wm==null)return;
        for(int i=0;i<3;i++){
            LinearLayout layout=newPanel();
            playerRows[i]=newCardText(11f);
            layout.addView(playerRows[i]);
            layout.setVisibility(View.GONE); // Hide players with no detected plays.
            panels[i]=layout;
        }
        // Keep only data-bearing rows. Unseen ranks already include the total count.
        LinearLayout bottom=newPanel();
        bottom.setPadding(dp(5),dp(3),dp(5),dp(3));
        handRow=newCardText(10f);
        bottom.addView(handRow);
        otherRow=newCardText(10f);
        bottom.addView(otherRow);
        remainingRow=newCardText(10f);
        bottom.addView(remainingRow);
        // Scanner status has its own tiny badge beside the Suggest button.
        // Do not add scan text or an extra background to the local card HUD.
        panels[3]=bottom;

        android.util.DisplayMetrics metrics=new android.util.DisplayMetrics();
        wm.getDefaultDisplay().getRealMetrics(metrics);
        sizeOverlayFonts(metrics.widthPixels,metrics.heightPixels);
        for(int i=0;i<panels.length;i++){
            WindowManager.LayoutParams params=new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    |WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    |WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                    |WindowManager.LayoutParams.FLAG_SECURE,
                PixelFormat.TRANSLUCENT);
            params.gravity=Gravity.TOP|Gravity.LEFT;
            // Android 12+ blocks touch-through for untrusted overlays with alpha > .8.
            params.alpha=.78f;
            panelParams[i]=params;
            wm.addView(panels[i],params);
        }
        createResetButton();
        createSuggestControls();
        createScanBadge();
        refresh();
    }
    /** The top-left reset control is its own tiny touch target; HUD remains pass-through. */
    private void createResetButton(){
        resetButton=new TextView(this);
        resetButton.setText("↻");
        resetButton.setContentDescription("Reset bài – bắt đầu ván mới");
        resetButton.setGravity(Gravity.CENTER);
        resetButton.setTextColor(0xffffffff);
        resetButton.setTextSize(22f);
        android.graphics.drawable.GradientDrawable bg=new android.graphics.drawable.GradientDrawable();
        bg.setColor(0xdd222a36);bg.setCornerRadius(dp(12));
        bg.setStroke(dp(1),0xffb9c2cd);
        resetButton.setBackground(bg);
        resetButton.setOnClickListener(v->manualReset());
        resetButton.setOnLongClickListener(v->{forceRescan();
            Toast.makeText(this,"Đang quét lại, giữ nguyên lịch sử",Toast.LENGTH_SHORT).show();
            return true;});
        resetButtonParams=new WindowManager.LayoutParams(dp(42),dp(42),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                |WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                |WindowManager.LayoutParams.FLAG_SECURE,
            PixelFormat.TRANSLUCENT);
        resetButtonParams.gravity=Gravity.TOP|Gravity.LEFT;
        resetButtonParams.alpha=.90f;
        placeResetButton();
        wm.addView(resetButton,resetButtonParams);
    }
    /** Manual repeat suggestion; automatic advice is shown once at each detected turn. */
    private void createSuggestControls(){
        suggestButton=new TextView(this);
        suggestButton.setText("Gợi ý");
        suggestButton.setContentDescription("Bấm để quét và xem lại gợi ý");
        suggestButton.setGravity(Gravity.CENTER);
        suggestButton.setTextColor(0xffeafce8);
        suggestButton.setTextSize(13f);
        suggestButton.setTypeface(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.BOLD);
        android.graphics.drawable.GradientDrawable bg=new android.graphics.drawable.GradientDrawable();
        bg.setColor(0xee204735);bg.setCornerRadius(dp(10));
        bg.setStroke(dp(1),0xff9ad2a6);
        suggestButton.setBackground(bg);
        suggestButtonParams=new WindowManager.LayoutParams(dp(59),dp(42),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                |WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                |WindowManager.LayoutParams.FLAG_SECURE,
            PixelFormat.TRANSLUCENT);
        suggestButtonParams.gravity=Gravity.TOP|Gravity.LEFT;
        suggestButtonParams.alpha=.95f;
        suggestButtonParams.x=dp(52);suggestButtonParams.y=dp(6);
        suggestButton.setOnClickListener(v->onSuggestClick());
        wm.addView(suggestButton,suggestButtonParams);

        LinearLayout frame=newPanel();
        frame.setPadding(dp(8),dp(6),dp(8),dp(6));
        suggestionText=newCardText(14);
        suggestionText.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        suggestionText.setTextColor(0xffffffff);
        frame.addView(suggestionText);
        suggestionPanel=frame;
        suggestionPanel.setVisibility(View.GONE);
        suggestionPanelParams=new WindowManager.LayoutParams(dp(345),
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                |WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                |WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                |WindowManager.LayoutParams.FLAG_SECURE,
            PixelFormat.TRANSLUCENT);
        suggestionPanelParams.gravity=Gravity.TOP|Gravity.LEFT;
        // Keep alpha <= .8 so the non-touchable popup cannot intercept game taps.
        suggestionPanelParams.alpha=.78f;
        wm.addView(suggestionPanel,suggestionPanelParams);
        placeSuggestionControls();
        wm.updateViewLayout(suggestionPanel,suggestionPanelParams);
    }
    /** Tiny diagnostic, above the table, next to Gợi ý; touch passes through. */
    private void createScanBadge(){
        scanBadge=new TextView(this);
        scanBadge.setText("Quét: chờ");
        scanBadge.setGravity(Gravity.CENTER_VERTICAL);
        scanBadge.setTextColor(0xfff5f8ef);
        scanBadge.setTextSize(11f);
        scanBadge.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        scanBadge.setPadding(dp(4),0,dp(4),0);
        // No rounded panel/border: just one short, legible line.
        scanBadge.setBackgroundColor(0x44212b35);
        scanBadge.setSingleLine(true);
        scanBadge.setEllipsize(android.text.TextUtils.TruncateAt.END);
        scanBadgeParams=new WindowManager.LayoutParams(dp(136),dp(29),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                WindowManager.LayoutParams.FLAG_SECURE,
            PixelFormat.TRANSLUCENT);
        scanBadgeParams.gravity=Gravity.TOP|Gravity.LEFT;
        scanBadgeParams.alpha=.72f;
        placeScanBadge();
        wm.addView(scanBadge,scanBadgeParams);
    }
    private void placeScanBadge(){
        if(scanBadgeParams!=null){scanBadgeParams.x=dp(118);scanBadgeParams.y=dp(12);}
    }
    private void setScanStatus(String value){
        scanDiagnosis=value;
        if(scanBadge!=null){
            mainHandler.post(()->{if(scanBadge!=null)scanBadge.setText("Quét: "+value);});
        }
    }
    /** User request starts a new read of hand, live board and count history.
     * No stored text from an earlier table position is used for advice.
     */
    private void onSuggestClick(){
        if(suggestionPanel==null||suggestionText==null)return;
        synchronized(stateLock){suggestRequested=true;suggestionScanFrames=0;}
        showPendingAdvice("ĐANG QUÉT BÀI…");
    }
    private void updateBoard(Collection<String> latest){
        ArrayList<String> cards=new ArrayList<>(latest);
        Collections.sort(cards);
        String key=android.text.TextUtils.join(" ",cards);
        if(key.equals(boardCandidate))boardFrames++;
        else{boardCandidate=key;boardFrames=1;boardStable=false;}
        if(boardFrames>=2){currentBoard=cards;boardStable=true;}
    }
    /** Only a complete and stable visible fan may repair a stale tracked hand.
     * The previous full 13-card scan remains the default way to initialize a deal.
     */
    private boolean updateVisibleHand(HandRecognizer.Result r){
        if(!r.complete || r.cards.isEmpty()){
            visualHandTrusted=false;visualFrames=0;visualCandidate="";return false;
        }
        ArrayList<String> cards=new ArrayList<>(r.cards);Collections.sort(cards);
        String key=android.text.TextUtils.join(" ",cards);
        if(key.equals(visualCandidate))visualFrames++;
        else{visualCandidate=key;visualFrames=1;visualHandTrusted=false;}
        if(visualFrames<3)return false;
        visualHandTrusted=true;lastVisualHandSize=cards.size();
        LinkedHashSet<String> scanned=new LinkedHashSet<>(cards);
        if(scanned.equals(handCards))return false;
        // Before the first verified local play, an apparent 8-12 card fan is
        // frequently a partially occluded 13-card hand. Do not promote it.
        if(handCards.isEmpty() && myPlayed.isEmpty() && scanned.size()!=13)return false;
        // Never allow visible cards to conflict with known played cards.
        for(String c:scanned)if(played.contains(c))return false;
        PartialDeckState.Result deck=PartialDeckState.check(
            scanned,observedCards(),knownCounts);
        if(!deck.feasible)return false;
        // The visible hand may be valid even when a few earlier plays were
        // missed; do not prevent recovery just because the history is partial.

        // A hand successfully conserved across all 52 known cards is stronger
        // evidence than an older manual choice; reconcile without guessing.
        handCards.clear();handCards.addAll(scanned);
        manualHandOverride=false;
        persistCurrentHand();
        getSharedPreferences("tracker_strategy",MODE_PRIVATE).edit().putString("hand_source","auto").apply();
        return true;
    }
    /** Visible-but-not-yet-attributed cards are still known cards for conservation.
     * Attribution to an opponent remains separate from deck integrity. */
    private LinkedHashSet<String> observedCards(){
        LinkedHashSet<String> visible=new LinkedHashSet<>(played);
        visible.addAll(waitingCards);
        return visible;
    }
    private static boolean conserved(Collection<String> hand,Collection<String> played,int[] counts){
        return CardStateValidator.complete(hand,played,counts);
    }
    private static boolean hasHeo(Collection<String> hand){
        for(String c:hand)if(c.startsWith("2"))return true;
        return false;
    }
    /** Snapshot and analyze off the screen-capture thread. Never block new video frames. */
    private void runFreshAdvice(){
        final LinkedHashSet<String> hand,seen;
        final List<String> lead;
        final int[] counts;
        final List<List<List<String>>> histories=new ArrayList<>();
        final boolean turn,boardOK,consistent,handOK;
        final int missingHistory;
        final int epoch,request;
        synchronized(stateLock){
            turn=myTurn;
            boardOK=boardStable;
            hand=new LinkedHashSet<>(handCards);
            seen=observedCards();
            lead=new ArrayList<>(currentBoard);
            counts=knownCounts.clone();
            // Full saved verified play history of the current occupants.
            // Do not infer an unobserved pass or secretly see hidden hands.
            for(LearnedOpponentHistory h:learning)histories.add(h.snapshot());
            PartialDeckState.Result check=PartialDeckState.check(hand,seen,counts);
            consistent=check.feasible && check.missing<=24;
            missingHistory=check.missing;
            // A visually confirmed full fan remains usable even when played
            // cards were missed; only overlapping/impossible cards invalidate it.
            handOK=!hand.isEmpty() && (visualHandTrusted || !handPending.isEmpty()
                || manualHandOverride);
            epoch=turnEpoch;
            request=++adviceRequestId;
        }
        adviceExecutor.execute(()->{
            final MonteCarloAdvisor.Advice result;
            final String heoWarning=HeoWarningAdvisor.warning(hand,seen);
            if(!turn)result=new MonteCarloAdvisor.Advice("CHƯA ĐẾN LƯỢT","");
            else if(!boardOK)result=new MonteCarloAdvisor.Advice("QUÉT LẠI BÀI GIỮA BÀN","");
            else if(!handOK||!consistent)result=new MonteCarloAdvisor.Advice(
                "QUÉT LẠI BÀI VÀ LỊCH SỬ","");
            else {
                MonteCarloAdvisor.Advice computed;
                try {
                    StrategyMode mode=StrategyMode.fromKey(getSharedPreferences("tracker_strategy",MODE_PRIVATE)
                        .getString("mode",StrategyMode.PLACEMENT.key));
                    computed=MonteCarloAdvisor.adviseBehaviorAware(hand,lead,seen,counts,histories,mode);
                }catch(Exception err){
                    android.util.Log.w("Tracker","Advice unavailable",err);
                    computed=new MonteCarloAdvisor.Advice("QUÉT LẠI BÀI VÀ LỊCH SỬ","");
                }
                result=computed;
            }
            mainHandler.post(()->{
                synchronized(stateLock){
                    if(stopping || !myTurn || turnEpoch!=epoch || adviceRequestId!=request)return;
                }
                showAdvice(result,heoWarning);
            });
        });
    }
    private void showPendingAdvice(String message){
        // Never put the scanner placeholder over cards, opponents or controls.
        setScanStatus("đang xử lý");
    }
    private void showUrgentHeoWarning(String text){
        if(suggestionPanel==null||suggestionText==null)return;
        suggestionText.setTextColor(0xffff8888);
        suggestionText.setText(text);
        if(dismissSuggestion!=null)mainHandler.removeCallbacks(dismissSuggestion);
        suggestionPanel.setVisibility(View.VISIBLE);
        dismissSuggestion=()->{if(suggestionPanel!=null)suggestionPanel.setVisibility(View.GONE);};
        mainHandler.postDelayed(dismissSuggestion,5000);
    }
    private void showAdvice(MonteCarloAdvisor.Advice advice,String warning){
        if(suggestionPanel==null||suggestionText==null)return;
        if(advice.title.startsWith("QUÉT LẠI")){
            setScanStatus("thiếu bài / lịch sử");
            if(warning!=null && !warning.isEmpty())showUrgentHeoWarning(warning);
            else if(suggestionPanel!=null)suggestionPanel.setVisibility(View.GONE);
            return;
        }
        setScanStatus("đã xử lý");
        String displayed=advice.title+(advice.detail.isEmpty()?"":"\n"+advice.detail);
        if(warning!=null && !warning.isEmpty() && !displayed.contains("⚠ HEO"))
            displayed+="\n"+warning;
        SpannableStringBuilder highlight=new SpannableStringBuilder(displayed);
        int warningStart=displayed.indexOf("⚠ HEO");
        if(warningStart>=0){
            highlight.setSpan(new ForegroundColorSpan(0xffff777b),warningStart,
                displayed.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            highlight.setSpan(new StyleSpan(android.graphics.Typeface.BOLD),warningStart,
                displayed.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        suggestionText.setTextColor(advice.title.startsWith("ĐÁNH")?0xffa6ffb6:
            advice.title.startsWith("BỎ")?0xffffdf94:0xffffffff);
        suggestionText.setText(highlight);
        if(dismissSuggestion!=null)mainHandler.removeCallbacks(dismissSuggestion);
        if(panels[3]!=null)panels[3].setVisibility(View.GONE);
        suggestionPanel.setVisibility(View.VISIBLE);
        dismissSuggestion=()->{
            if(suggestionPanel!=null)suggestionPanel.setVisibility(View.GONE);
            if(panels[3]!=null)panels[3].setVisibility(View.VISIBLE);
        };
        mainHandler.postDelayed(dismissSuggestion,5000);
    }
    private void placeSuggestionControls(){
        if(wm==null)return;
        android.util.DisplayMetrics dm=new android.util.DisplayMetrics();
        wm.getDefaultDisplay().getRealMetrics(dm);
        int screen=dm.widthPixels;
        if(suggestButtonParams!=null){
            suggestButtonParams.x=dp(52);suggestButtonParams.y=dp(6);
        }
        if(suggestionPanelParams!=null){
            android.util.DisplayMetrics dm2=new android.util.DisplayMetrics();
            wm.getDefaultDisplay().getRealMetrics(dm2);
            boolean landscape=dm2.widthPixels>dm2.heightPixels;
            suggestionPanelParams.width=landscape?
                Math.max(dp(165),Math.min(dp(360),(int)(screen*.37f))):
                Math.max(dp(165),(int)(screen*.8f));
            // Advice (not scan state) uses small top-left overlay, well clear of hand.
            suggestionPanelParams.x=landscape?dp(258):dp(6);
            suggestionPanelParams.y=landscape?dp(8):dp(55);
        }
    }
    private void placeResetButton(){
        if(resetButtonParams==null)return;
        resetButtonParams.x=dp(6);
        resetButtonParams.y=dp(6);
    }
    private void relayoutOverlays(int width,int height){
        if(wm==null||width<=0||height<=0)return;
        sizeOverlayFonts(width,height);
        updateOverlayLayout(width,height);
        if(resetButton!=null && resetButtonParams!=null){
            placeResetButton();
            try{wm.updateViewLayout(resetButton,resetButtonParams);}
            catch(Exception ex){android.util.Log.w("Tracker","Relayout reset",ex);}
        }
        placeSuggestionControls();
        if(suggestButton!=null&&suggestButtonParams!=null)try{wm.updateViewLayout(suggestButton,suggestButtonParams);}catch(Exception ignored){}
        if(suggestionPanel!=null&&suggestionPanelParams!=null)try{wm.updateViewLayout(suggestionPanel,suggestionPanelParams);}catch(Exception ignored){}
        placeScanBadge();
        if(scanBadge!=null&&scanBadgeParams!=null)try{wm.updateViewLayout(scanBadge,scanBadgeParams);}catch(Exception ignored){}
    }
    private void sizeOverlayFonts(int w,int h){
        float side=Math.min(w,h);
        for(int i=0;i<3;i++){
            if(playerRows[i]!=null)playerRows[i].setTextSize(TypedValue.COMPLEX_UNIT_PX,Math.max(18f,side*.021f));
        }
        if(handRow!=null)handRow.setTextSize(TypedValue.COMPLEX_UNIT_PX,Math.max(16f,side*.018f));
        if(otherRow!=null)otherRow.setTextSize(TypedValue.COMPLEX_UNIT_PX,Math.max(16f,side*.018f));
        if(remainingRow!=null)remainingRow.setTextSize(TypedValue.COMPLEX_UNIT_PX,Math.max(17f,side*.019f));
    }
    private int desiredWidth(TextView text){
        if(text==null||text.getVisibility()!=View.VISIBLE)return 0;
        return (int)Math.ceil(text.getPaint().measureText(text.getText().toString()));
    }
    /** Width follows visible text. Height grows/shrinks after wrapping, without a scroll view. */
    private void updateOverlayLayout(int width,int height){
        if(wm==null)return;
        boolean landscape=width>=height;
        for(int i=0;i<panels.length;i++){
            View v=panels[i];WindowManager.LayoutParams p=panelParams[i];
            if(v==null||p==null)continue;
            int maxWidth=landscape?(int)(width*(i==3?.575f:.275f)):(int)(width*.94f);
            int minWidth=(i==3)?dp(114):dp(40);
            int needed;
            if(i<3)needed=desiredWidth(playerRows[i]);
            else needed=Math.max(desiredWidth(remainingRow),Math.max(desiredWidth(handRow),desiredWidth(otherRow)));
            // The reset control occupies its own window, not HUD content.
            p.width=Math.min(maxWidth,Math.max(minWidth,needed+dp(20)));
            p.height=WindowManager.LayoutParams.WRAP_CONTENT;
            if(landscape){
                // Keep P1 history BELOW their red nickname ribbon (~24–28% screen height)
                // so MediaProjection can read the name for automatic player replacement.
                if(i==0){p.x=(width-p.width)/2;p.y=(int)(height*.305f);}
                // Side players sit ABOVE their avatars/card counters to free mid-table space.
                else if(i==1){p.x=(int)(width*.014f);p.y=(int)(height*.145f);}
                else if(i==2){p.x=width-p.width-(int)(width*.014f);p.y=(int)(height*.145f);}
                else{
                    p.x=(int)(width*.014f);
                    // Measure actual wrapped text: bottom edge stays ABOVE the hand.
                    int measureW=View.MeasureSpec.makeMeasureSpec(p.width,View.MeasureSpec.EXACTLY);
                    int measureH=View.MeasureSpec.makeMeasureSpec((int)(height*.36f),View.MeasureSpec.AT_MOST);
                    v.measure(measureW,measureH);
                    p.y=Math.max((int)(height*.35f),(int)(height*.657f)-v.getMeasuredHeight());
                }
            }else{
                p.x=(int)(width*.03f);
                p.y=(int)(height*(.12f+i*.16f));
            }
            try{wm.updateViewLayout(v,p);}
            catch(Exception ex){android.util.Log.w("Tracker","Relayout HUD "+i,ex);}
        }
    }
    private void refresh(){
        if(remainingRow==null)return;
        synchronized(stateLock){
            for(int i=0;i<3;i++){
                int n=byPlayer[i].size();
                View p=panels[i];
                // Spatial location identifies the player. No redundant P1/P2/P3 label.
                if(playerRows[i]!=null)playerRows[i].setText(recentPlaySpans(playerHistory[i]));
                if(p!=null)p.setVisibility(n>0?View.VISIBLE:View.GONE);
            }
            if(handCards.isEmpty())handRow.setVisibility(View.GONE);
            else{
                handRow.setVisibility(View.VISIBLE);
                // Compact grouped rank display: 6♠♥ instead of 6♠ 6♥.
                StringBuilder mine=new StringBuilder("Tôi (").append(handCards.size()).append("): ");
                String[] suits={"S","C","D","H"};
                for(String rank:CardWarnings.RANKS){
                    StringBuilder group=new StringBuilder(rank);
                    int n=0;
                    for(String suit:suits)if(handCards.contains(rank+suit)){
                        group.append(suitSymbol(suit));n++;
                    }
                    if(n>0)mine.append(group).append("  ");
                }
                handRow.setText(mine.toString());
            }
            if(otherPlayed.isEmpty())otherRow.setVisibility(View.GONE);
            else{
                otherRow.setVisibility(View.VISIBLE);
                otherRow.setText(recentPlaySpans(otherHistory, "Khác  "));
            }
            List<String> remaining=remainingCards();
            // Risk colors must use cards opponents COULD hold, not my known cards.
            ArrayList<String> unknown=new ArrayList<>(remaining);
            unknown.removeAll(handCards);
            remainingRow.setText(buildRemainingSpans(remaining,unknown));
            // No scanner diagnostic row here: it crowds the fan and obscures play.
        }
        android.util.DisplayMetrics metrics=new android.util.DisplayMetrics();
        if(wm!=null){
            wm.getDefaultDisplay().getRealMetrics(metrics);
            updateOverlayLayout(metrics.widthPixels,metrics.heightPixels);
        }
    }
    /** Chronological history: oldest at left, newest at right (yellow). */
    private CharSequence recentPlaySpans(RecentMoves history){
        return recentPlaySpans(history, "");
    }
    private CharSequence recentPlaySpans(RecentMoves history,String prefix){
        SpannableStringBuilder sb=new SpannableStringBuilder(prefix);
        String older=history.olderMoves();
        if(!older.isEmpty()){
            int start=sb.length();
            sb.append(older).append("  ›  ");
            sb.setSpan(new ForegroundColorSpan(0xffd8e4f3),start,sb.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        String latest=history.mostRecent();
        int start=sb.length();
        sb.append(latest);
        if(!latest.isEmpty()){
            sb.setSpan(new ForegroundColorSpan(0xffffe699),start,sb.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            sb.setSpan(new StyleSpan(android.graphics.Typeface.BOLD),start,sb.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return sb;
    }
    /** Green: rank can participate in a run of 3/4 consecutive remaining pairs.
     * Red takes priority if all 4 suits of the rank remain unseen.
     * This is a *possibility across the unobserved pool*, NOT proof that
     * one opponent actually holds the full combination.
     */
    private CharSequence buildRemainingSpans(Collection<String> remaining,Collection<String> unknown){
        CardWarnings.Result warnings=CardWarnings.analyze(unknown);
        HashSet<String> unseen=new HashSet<>(remaining);
        SpannableStringBuilder sb=new SpannableStringBuilder();
        sb.append("Chưa đánh (").append(String.valueOf(remaining.size())).append("): ");
        if(warnings.hasTriple||warnings.hasQuad){
            int a=sb.length();
            if(warnings.hasTriple)sb.append("3ĐT");
            if(warnings.hasTriple&&warnings.hasQuad)sb.append("/");
            if(warnings.hasQuad)sb.append("4ĐT");
            sb.append("  ");
            sb.setSpan(new ForegroundColorSpan(0xffa9ffb5),a,sb.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            sb.setSpan(new StyleSpan(android.graphics.Typeface.BOLD),a,sb.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        String[] ranks=CardWarnings.RANKS;
        String[] suits={"S","C","D","H"};
        for(int i=0;i<ranks.length;i++){
            String rank=ranks[i];
            int count=0;
            for(String suit:suits)if(unseen.contains(rank+suit))count++;
            if(count==0)continue;
            int start=sb.length();
            sb.append(rank);
            for(String suit:suits){
                if(unseen.contains(rank+suit))sb.append(suitSymbol(suit));
            }
            if(count==4 && warnings.remainingByRank[i]==4){
                sb.setSpan(new ForegroundColorSpan(0xffff8585),start,sb.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                sb.setSpan(new BackgroundColorSpan(0xff49242c),start,sb.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                sb.setSpan(new StyleSpan(android.graphics.Typeface.BOLD),start,sb.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }else if(warnings.inQuad[i]||warnings.inTriple[i]){
                sb.setSpan(new ForegroundColorSpan(0xffa5ffbd),start,sb.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                sb.setSpan(new BackgroundColorSpan(0xff194b35),start,sb.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                sb.setSpan(new StyleSpan(android.graphics.Typeface.BOLD),start,sb.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                if(warnings.inQuad[i])sb.setSpan(new UnderlineSpan(),start,sb.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            sb.append("  ");
        }
        return sb;
    }
    private static String suitSymbol(String s){
        switch(s){
            case "S": return "♠";
            case "C": return "♣";
            case "D": return "♦";
            case "H": return "♥";
            default: return "?";
        }
    }
    private static String pretty(String s){
        if(s.isEmpty())return s;
        String rank=s.substring(0,s.length()-1),suit=s.substring(s.length()-1);
        String symbol=suit.equals("S")?"♠":suit.equals("C")?"♣":suit.equals("H")?"♥":"♦";
        return rank+symbol;
    }
    private List<String> remainingCards(){
        ArrayList<String> remain=new ArrayList<>();
        String[] ranks={"3","4","5","6","7","8","9","10","J","Q","K","A","2"};
        String[] suits={"S","C","D","H"};
        for(String r:ranks)for(String s:suits){
            String c=r+s;
            if(!played.contains(c))remain.add(c);
        }
        return remain;
    }
    @Override public void onDestroy(){
        stopping=true;
        mainHandler.removeCallbacks(captureWatchdog);
        adviceExecutor.shutdownNow();
        if(imageReader!=null){imageReader.setOnImageAvailableListener(null,null);}
        if(virtualDisplay!=null){virtualDisplay.release();virtualDisplay=null;}
        if(imageReader!=null){imageReader.close();imageReader=null;}
        MediaProjection old=projection;projection=null;
        if(old!=null)try{old.stop();}catch(Exception ignored){}
        if(worker!=null){worker.quitSafely();worker=null;}
        if(wm!=null)for(int i=0;i<panels.length;i++){
            if(panels[i]!=null)try{wm.removeView(panels[i]);}catch(Exception ignored){}
            panels[i]=null;panelParams[i]=null;
        }
        if(wm!=null && resetButton!=null)try{wm.removeView(resetButton);}catch(Exception ignored){}
        resetButton=null;resetButtonParams=null;
        if(dismissSuggestion!=null)mainHandler.removeCallbacks(dismissSuggestion);
        if(wm!=null&&suggestButton!=null)try{wm.removeView(suggestButton);}catch(Exception ignored){}
        if(wm!=null&&suggestionPanel!=null)try{wm.removeView(suggestionPanel);}catch(Exception ignored){}
        if(wm!=null&&scanBadge!=null)try{wm.removeView(scanBadge);}catch(Exception ignored){}
        scanBadge=null;scanBadgeParams=null;
        suggestButton=null;suggestButtonParams=null; suggestionText=null;suggestionPanel=null;suggestionPanelParams=null;
        Arrays.fill(playerRows,null);
        handRow=null;otherRow=null;remainingRow=null;
        super.onDestroy();
    }
    @Override public android.os.IBinder onBind(Intent i){return null;}
}
