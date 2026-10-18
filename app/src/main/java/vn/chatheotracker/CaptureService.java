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
import android.text.style.RelativeSizeSpan;
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
    static final String ACTION_CONFIRM_PASS="vn.chatheotracker.CONFIRM_PASS";
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
    private final TextView[] threatRows=new TextView[3];
    private final String[] threatLabels={"3ĐT — · 4ĐT — · TQ —", "3ĐT — · 4ĐT — · TQ —", "3ĐT — · 4ĐT — · TQ —"};
    private final List<ObservedPass>[] confirmedPasses=new List[]{new ArrayList<>(),new ArrayList<>(),new ArrayList<>()};
    private boolean threatDirty=true;
    private long lastThreatScan=0L;
    private int threatEpoch=0;
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
    // Once a hand is confirmed, a momentarily obstructed view must not discard
    // the fact that the stored hand was verified earlier in this round.
    private boolean handEverVerified=false;
    // Fresh screenshot observations, never stale manual table text.
    private List<String> currentBoard=Collections.emptyList();
    private String boardCandidate="";
    private int boardFrames=0;
    private boolean boardStable=false;
    private boolean myTurn=false;
    private int turnFrames=0;
    private final ExecutorService adviceExecutor=Executors.newSingleThreadExecutor();
    // Slow threat sampling must never delay a time-sensitive move suggestion.
    private final ExecutorService threatExecutor=Executors.newSingleThreadExecutor();
    private final java.util.concurrent.atomic.AtomicBoolean threatTaskRunning=
        new java.util.concurrent.atomic.AtomicBoolean(false);
    private final AdviceReentryDetector adviceReentry=new AdviceReentryDetector();
    private final AdviceRetryGate adviceRetryGate=new AdviceRetryGate();
    private final BaitBehaviorSignals baitSignals=new BaitBehaviorSignals();
    private String confirmedPassLeadCleared="";
    private String lastLoggedThreatSignature="";
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
    private final RedealDetector redealDetector=new RedealDetector();
    private final HandEndVisibility handEndVisibility=new HandEndVisibility();
    private boolean resultLikeNoHand=false;
    private long ignoreCardsUntil=0L;
    private boolean stopping=false;
    private int densityDpi;
    private TrainingDataStore trainingData;
    private DiagnosticLog diagnostics;
    private String lastDiagnosticHand="";
    private String lastBoardLayerSignature="";
    private long lastBoardLayerAt=0;
    private long lastDiagnosticHandAt=0;
    private String roundId;
    private String lastPartialLogged="";
    private long lastPartialLoggedAt=0;

    /** Training is observational: never infer hidden cards or label a proposed move as a play. */
    private void trainEvent(String name,Object... attributes){
        if(trainingData==null || roundId==null)return;
        if(!getSharedPreferences("tracker_training",MODE_PRIVATE).getBoolean("collect",true))return;
        trainingData.append(name,roundId,TrainingDataStore.fields(attributes));
    }
    private void diag(String event,Object... data){
        if(diagnostics!=null)diagnostics.event(event,roundId,data);
    }
    private void startNewRound(String reason){
        roundId=UUID.randomUUID().toString();
        getSharedPreferences("tracker_training",MODE_PRIVATE).edit()
            .putString("active_round_id",roundId).apply();
        trainEvent("round_begin","reason",reason);
    }
    @Override public void onCreate(){
        super.onCreate();mainHandler=new Handler(Looper.getMainLooper());
        trainingData=new TrainingDataStore(getFilesDir());
        diagnostics=new DiagnosticLog(getFilesDir());
        // Each new screen-capture session must have its own round ID; resuming
        // the app must not silently blend observations from separate games.
        roundId=null;
    }
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
        if(ACTION_CONFIRM_PASS.equals(action)){
            int seat=intent.getIntExtra(EXTRA_PLAYER,-1);
            if(seat>=0&&seat<3) synchronized(stateLock){
                // No automated claim from a missed animation. Only record a user-
                // confirmed pass with a verified current public lead.
                if(boardStable && !currentBoard.isEmpty() && roundId!=null){
                    trainEvent("opponent_pass","seat",seat,
                        "lead",new ArrayList<>(currentBoard),
                        "lead_is_two",currentBoard.size()==1 && hasHeo(currentBoard),
                        "opponent_counts",knownCounts.clone(),
                        "confidence","user_confirmed", "source","user_confirmation");
                    if(knownCounts[seat]>=0){
                        List<ObservedPass> history=confirmedPasses[seat];
                        // Avoid double counting repeated button taps on the same lead.
                        if(history.isEmpty() || !history.get(history.size()-1).lead.equals(currentBoard)
                                || history.get(history.size()-1).countAtPass!=knownCounts[seat]){
                            history.add(new ObservedPass(currentBoard,knownCounts[seat]));
                            if(history.size()>8)history.remove(0);
                            threatDirty=true;
                        }
                    }
                    // Optional, fully user-confirmed fallback: if our last
                    // verified play was followed by three confirmed passes,
                    // the next suggestion is an OPENING, even if the old cards
                    // remain painted on the table during the transition.
                    boolean allPassed=true;
                    for(int p=0;p<3;p++){
                        boolean found=false;
                        for(ObservedPass pass:confirmedPasses[p]){
                            if(pass.lead.equals(currentBoard) && pass.countAtPass==knownCounts[p]){
                                found=true;break;
                            }
                        }
                        if(!found)allPassed=false;
                    }
                    if(allPassed && myPlayed.containsAll(currentBoard) &&
                            !boardSignature(currentBoard).equals(confirmedPassLeadCleared)){
                        confirmedPassLeadCleared=boardSignature(currentBoard);
                        if(myTurn){
                            turnEpoch++;
                            autoAdvicePending=true;autoAdviceScanFrames=0;
                            adviceRequestId++;
                        }
                        trainEvent("advice_reentry","reason","three_user_confirmed_passes",
                            "confidence","user_confirmed_on_own_lead");
                    }
                }
            }
            if(projection==null)stopSelf();
            return START_NOT_STICKY;
        }
        if(ACTION_SET_HAND.equals(action)){
            StrategyAdvisor.Input candidate=StrategyAdvisor.parse(intent.getStringExtra(EXTRA_HAND_TEXT));
            if(candidate.error==null && !candidate.cards.isEmpty()){
                synchronized(stateLock){
                    handCards.clear();handCards.addAll(candidate.cards);
                    manualHandOverride=true;handEverVerified=true;
                    handPending="";handPendingFrames=0;
                    visualHandTrusted=false;visualFrames=0;
                    persistCurrentHand();
                    getSharedPreferences("tracker_strategy",MODE_PRIVATE).edit()
                        .putString("hand_source","manual").apply();
                    adviceRequestId++;
                    threatEpoch++;threatDirty=true;lastThreatScan=0;
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
                threatEpoch++;threatDirty=true;lastThreatScan=0;
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
        startNewRound("capture_session_started");
        diag("capture_started","reason","new_permission_session");
        recognizer=new CardRecognizer(this);
        // Restore explicitly edited hand only. Previous automatic scans may be stale.
        android.content.SharedPreferences pref=getSharedPreferences("tracker_strategy",MODE_PRIVATE);
        String savedSource=pref.getString("hand_source", "");
        if("manual".equals(savedSource)){
            StrategyAdvisor.Input previously=StrategyAdvisor.parse(pref.getString("hand",""));
            if(previously.error==null){
                synchronized(stateLock){handCards.addAll(previously.cards);manualHandOverride=true;handEverVerified=true;}
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
                // Only the bright ACTIVE layer can add played-card evidence.
                // The combined detector also sees old gray cards; logging those
                // as new moves caused 2H to be credited while 2C was on top.
                updateBoard(whiteCards);
                LinkedHashSet<String> historicalLayer=new LinkedHashSet<>(cards);
                historicalLayer.removeAll(whiteCards);
                if(!historicalLayer.isEmpty()){
                    String layerKey=historicalLayer.toString()+"|"+whiteCards;
                    if(!layerKey.equals(lastBoardLayerSignature)||now-lastBoardLayerAt>12000){
                        diag("board_layers_filtered","historical_only",new ArrayList<>(historicalLayer),
                            "active_white",whiteCards,"counts",knownCounts.clone());
                        lastBoardLayerSignature=layerKey;lastBoardLayerAt=now;
                    }
                }
                changed=handleFrame(new ArrayList<>(ActiveBoardEvidence.active(cards,whiteCards)),counts,now) || occupantChanged;
                // Seven full-count frames may be a new deal. Until the
                // detector decides, keep the old hand/ledger immutable.
                if(myCards!=null && !redealDetector.awaitingConfirmation()){
                    String frameSignature=myCards.fanCount+":"+myCards.cards+":"+myCards.complete+":"+myCards.diagnosis;
                    if(!frameSignature.equals(lastDiagnosticHand) || now-lastDiagnosticHandAt>12000){
                        diag("hand_scan","cards",myCards.cards,"slots",myCards.slots,
                            "fan_count",myCards.fanCount,"recognized",myCards.cards.size(),
                            "complete",myCards.complete,"diagnosis",myCards.diagnosis,
                            "counts",knownCounts.clone(),"frame_index",scanFrameIndex);
                        lastDiagnosticHand=frameSignature;lastDiagnosticHandAt=now;
                    }
                    if(!myCards.complete && !myCards.cards.isEmpty()){
                        String sig=myCards.cards.size()+":"+myCards.diagnosis;
                        if(!sig.equals(lastPartialLogged)||now-lastPartialLoggedAt>10000){
                            trainEvent("hand_partial","visible_count",myCards.cards.size(),
                                "diagnosis",myCards.diagnosis,"confidence","incomplete_not_ground_truth");
                            lastPartialLogged=sig;lastPartialLoggedAt=now;
                        }
                    }
                    if(handleHandFrame(myCards))changed=true;
                    if(updateVisibleHand(myCards))changed=true;
                    boolean ended=handEndVisibility.observe(
                        myCards.complete && !myCards.cards.isEmpty(),knownCounts);
                    if(ended!=resultLikeNoHand){
                        resultLikeNoHand=ended;changed=true;
                        if(ended){
                            diag("hand_absent_after_winner","counts",knownCounts.clone(),
                                "reason","no_visible_fan_eight_scans_not_confirmed_result");
                            autoAdvicePending=false;suggestRequested=false;
                            mainHandler.post(()->{
                                if(suggestionPanel!=null)suggestionPanel.setVisibility(View.GONE);
                            });
                        }
                    }
                }
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
                // Some tables keep the green action control visible through a
                // sequence of confirmed passes, so the false->true button edge
                // can be lost. An occupied board that stably clears is another
                // indication that the next lead may be ours.
                if(adviceReentry.observe(myTurn,boardStable,currentBoard,now) &&
                        !autoAdvicePending){
                    turnEpoch++;
                    autoAdvicePending=true;
                    autoAdviceScanFrames=0;
                    turnBegan=true;
                    trainEvent("advice_reentry","reason","occupied_board_cleared",
                        "confidence","stable_public_board_not_pass_proof");
                }
                // If an earlier recommendation was withheld because the scan
                // was incomplete, retry automatically once REAL input changes.
                // No loop on the same missing-card state.
                if(myTurn && !autoAdvicePending && !suggestRequested &&
                    adviceRetryGate.shouldRetry(adviceFingerprint(),
                        boardStable && handEverVerified && !handCards.isEmpty(),now)){
                    autoAdvicePending=true;autoAdviceScanFrames=0;
                    trainEvent("advice_retry","reason","new_verified_scan_evidence",
                        "confidence","state_changed_not_a_new_play");
                }
                if(myTurn && autoAdvicePending){
                    autoAdviceScanFrames++;
                    // At least three post-turn frames for screen / card history stabilization.
                    // If counters/hand still mismatch, keep scanning for up to ~3 seconds.
                    boolean ready=boardStable && !handCards.isEmpty() && handEverVerified;
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
                    if(resultLikeNoHand)state="chờ ván mới · giữ log";
                    else if(!handCards.isEmpty()){
                        PartialDeckState.Result check=PartialDeckState.check(
                            handCards,observedCards(),knownCounts);
                        if(check.exact)state=handCards.size()+" lá ✓";
                        else if(check.feasible)
                            state=handCards.size()+" lá · thiếu lịch sử "+check.missing;
                        else {
                            ObservedHandReconciler.Result partial=ObservedHandReconciler.inspect(
                                handCards,observedCards(),knownCounts);
                            state=partial.feasible && !partial.allCountersKnown
                                ? handCards.size()+" lá ✓ tay · thiếu đếm"
                                : handCards.size()+" lá · chưa đối soát";
                        }
                    }else if(myCards!=null){
                        state=myCards.complete?(myCards.cards.size()+" lá · chưa đồng bộ"):
                            (myCards.fanCount>0?"góc "+myCards.cards.size()+"/"+myCards.fanCount:
                                myCards.diagnosis);
                    }else state="đã nhận hình";
                }
                setScanStatus(state);
            }
            if(changed){
                synchronized(stateLock){threatDirty=true;}
                mainHandler.post(this::refresh);
            }
            boolean shouldSnapshot=false;
            synchronized(stateLock){
                // Late-game card counts are more informative: recompute promptly.
                boolean endgame=false;
                for(int c:knownCounts)if(c>=0 && c<=5)endgame=true;
                long delay=endgame?300:1100;
                if(threatDirty && now-lastThreatScan>=delay){
                    threatDirty=false;lastThreatScan=now;shouldSnapshot=true;
                }
            }
            if(shouldSnapshot)scheduleThreatSnapshot();
            if(turnBegan)mainHandler.post(()->showPendingAdvice("ĐANG QUÉT BÀI…"));
            if(turnEnded)mainHandler.post(()->{
                if(dismissSuggestion!=null)mainHandler.removeCallbacks(dismissSuggestion);
                if(suggestionPanel!=null)suggestionPanel.setVisibility(View.GONE);
            });
            if(trySuggestion && !resultLikeNoHand)runFreshAdvice();
        }catch(Exception e){
            diag("frame_exception","exception_type",e.getClass().getSimpleName());
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
        handCards.addAll(sorted);handEverVerified=true;
        diag("hand_synced","cards",sorted,"reason","initial_thirteen");
        threatDirty=true;threatEpoch++;lastThreatScan=0;
        trainEvent("hand_verified","cards",sorted,"method","stable_three_frames",
            "confidence","verified_13","count",sorted.size());
        getSharedPreferences("tracker_strategy",MODE_PRIVATE).edit().putString("hand",signature).putString("hand_source","auto").apply();
        return true;
    }
    private void updateHandAfterPlay(Collection<String> cards){
        if(handCards.isEmpty())return;
        ArrayList<String> confirmedOverlap=new ArrayList<>();
        for(String c:cards){
            if(handCards.remove(c)){
                myPlayed.add(c);
                otherPlayed.remove(c);otherHistory.remove(c);
                confirmedOverlap.add(c);
            }
        }
        if(!confirmedOverlap.isEmpty()){
            // Record only the overlap actually removed from OUR verified hand;
            // the surrounding board may include other players' plays.
            trainEvent("hand_cards_removed","cards",confirmedOverlap,
                "confidence","observed_board_overlap");
            trainEvent("local_play_observed","cards",confirmedOverlap,
                "played_two",hasHeo(confirmedOverlap),
                "confidence","fan_and_board_overlap_not_button_proof");
            persistCurrentHand();
        }
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
            // A false winner badge can oscillate 2 -> 0 -> 2 in ~400 ms.
            // Require more real frames before accepting a terminal zero.
            if(candidateFrames[i]<CounterStabilityGate.requiredFrames(knownCounts[i],count)
                    || knownCounts[i]==count)continue;
            int before=knownCounts[i];
            knownCounts[i]=count;
            if(before!=count){
                changed=true;
                diag("opponent_count","seat",i,"before",before,"after",count);
                trainEvent("opponent_count","seat",i,"before",before,"after",count,
                    "confidence",count==0?"stable_four_frames":"stable_multiple_frames");
            }
            if(before>count){
                countDrops.add(new CountDrop(i,before-count,now));
                sawAnyLowerCounter=true;
                changed=true;
            }else if(before>=0 && count==13){changed=true;}
        }
        // The old logic depended on historical *recognized* counts and could miss
        // the next deal if a 0 counter vanished or a player's identity changed.
        // Independently require seven fresh frames at 13/13/13, armed only after
        // confirmed progress in the previous deal. Never reset an idle full table.
        boolean playedBefore= sawAnyLowerCounter || !played.isEmpty() ||
            !myPlayed.isEmpty() || !waitingCards.isEmpty() ||
            !cardMoves.isEmpty() || !countDrops.isEmpty();
        if(redealDetector.observe(counts,playedBefore)){
            diag("redeal_evidence","counts",counts.clone(),"reason","stable_counter_cycle");
            resetRoundInternal("confirmed_redeal");
            for(int i=0;i<3;i++){
                knownCounts[i]=13;lastCandidates[i]=13;candidateFrames[i]=2;
            }
            ignoreCardsUntil=now+1200;
            setScanStatus("ván mới · đang quét");
            return true;
        }
        if(now<ignoreCardsUntil || redealDetector.awaitingConfirmation()){
            recentCards.clear();
            return changed;
        }
        // A card may disappear for a few frames behind chat/emotes. Accumulate
        // per-card evidence over time instead of demanding identical full lists.
        LinkedHashSet<String> newlySeen=recentCards.update(visible,played,waitingCards,now);
        if(!newlySeen.isEmpty()){
            waitingCards.addAll(newlySeen);
            diag("board_fragment","cards",new ArrayList<>(newlySeen),"board",currentBoard,
                "counts",knownCounts.clone());
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
        // Never assign an incomplete 8-9-10 fragment to a 5-card counter
        // decrease. Wait for J-Q from later video frames and commit ONE move.
        for(int iteration=0;iteration<8;iteration++){
            ArrayList<PlayBatchMatcher.Fragment> fragments=new ArrayList<>();
            for(CardMove move:cardMoves)
                fragments.add(new PlayBatchMatcher.Fragment(move.cards,move.time,move.updated));
            ArrayList<PlayBatchMatcher.Drop> drops=new ArrayList<>();
            for(CountDrop drop:countDrops)
                drops.add(new PlayBatchMatcher.Drop(drop.player,drop.delta,drop.time));
            PlayBatchMatcher.Match match=PlayBatchMatcher.exact(fragments,drops,
                currentBoard,boardStable,now);
            if(match==null)break;
            CountDrop drop=countDrops.remove(match.dropIndex);
            ArrayList<String> accepted=new ArrayList<>();
            for(String card:match.cards)if(played.add(card)){
                byPlayer[drop.player].add(card);
                accepted.add(card);
            }
            for(int k=match.fragments.size()-1;k>=0;k--)
                cardMoves.remove((int)match.fragments.get(k));
            waitingCards.removeAll(match.cards);
            if(!accepted.isEmpty()){
                playerHistory[drop.player].record(accepted);
                BaitBehaviorSignals.Observation signal=baitSignals.record(drop.player,
                    accepted,handEverVerified?handCards:null,knownCounts[drop.player]);
                if(signal.possibleBaitSignal())trainEvent("bait_candidate","seat",drop.player,
                    "cards",accepted,"signal",signal.label(),"my_heo_verified",true,
                    "remaining_after",knownCounts[drop.player],
                    "confidence","weak_observable_pattern_not_hidden_intent");
                trainEvent("opponent_move","seat",drop.player,"cards",accepted,
                    "count_decrease",drop.delta,"remaining_after",knownCounts[drop.player],
                    "local_hand_count",handEverVerified?handCards.size():-1,
                    "local_heo_visible",handEverVerified && hasHeo(handCards),
                    "lead_previous_frame",new ArrayList<>(currentBoard),
                    "lead_context_confidence","not_verified_same_turn",
                    "opponent_counts",knownCounts.clone(),"confidence","matched_exact");
                learning[drop.player].record(accepted);
                persistOpponentLearning(drop.player);
            }
            diag("move_matched","seat",drop.player,"delta",drop.delta,
                "fragments",match.fragments.size(),"cards",accepted,"board",currentBoard);
            changed=true;
        }
        // Old code prematurely attributed 3/5 cards to a player (or to
        // 'Khác') and subsequently misattributed the other 2/5. Keep incomplete
        // batches uncertain instead of poisoning confirmed opponent history.
        for(int m=0;m<cardMoves.size();){
            CardMove move=cardMoves.get(m);
            if(now-move.updated<4800){m++;continue;}
            boolean pendingCounter=false;
            for(CountDrop drop:countDrops)
                if(Math.abs(drop.time-move.time)<3000){pendingCounter=true;break;}
            if(pendingCounter){m++;continue;}
            ArrayList<String> accepted=new ArrayList<>();
            for(String card:move.cards)if(played.add(card)){
                otherPlayed.add(card);accepted.add(card);
            }
            if(!accepted.isEmpty()){
                otherHistory.record(accepted);
                trainEvent("unattributed_play","cards",accepted,
                    "confidence","no_counter_match");
                diag("unattributed_play","cards",accepted,"reason","no_matching_counter");
            }
            updateHandAfterPlay(accepted);
            waitingCards.removeAll(move.cards);
            cardMoves.remove(m);changed=true;
        }
        for(int i=0;i<countDrops.size();){
            CountDrop drop=countDrops.get(i);
            if(now-drop.time>6500){
                trainEvent("missing_play","seat",drop.player,"count_decrease",drop.delta,
                    "confidence","unreadable_cards");
                diag("incomplete_move","seat",drop.player,"delta",drop.delta,
                    "reason","card_count_not_matched");
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
            trainEvent("seat_transition","seat",i,"transition",ev.name().toLowerCase(Locale.ROOT),
                "confidence","visual_only_unverified_identity");
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
                confirmedPasses[i].clear();threatDirty=true;
                for(int k=countDrops.size()-1;k>=0;k--)if(countDrops.get(k).player==i)countDrops.remove(k);
                adviceRequestId++;autoAdvicePending=myTurn;
                autoAdviceScanFrames=0;
                changed=true;
            }else if(ev==SeatIdentityTracker.Event.FIRST_SEEN){
                // Restore a previously seen player if they returned to this seat.
                trainingArchive.remember(oldIdentity,learning[i]);
                learning[i]=trainingArchive.restore(seatIdentity[i].active());
                confirmedPasses[i].clear();threatDirty=true;
                changed=true;
            }
            persistIdentity(i);
        }
        return changed;
    }

    private void resetRoundInternal(String reason){
        if(roundId!=null){
            trainEvent("round_end","result","unknown","reason",reason);
        diag("round_reset","reason",reason);
            getSharedPreferences("tracker_training",MODE_PRIVATE).edit()
                .putString("last_round_id",roundId).apply();
        }
        startNewRound(reason);
        lastPartialLogged="";lastPartialLoggedAt=0;
        lastDiagnosticHand="";lastDiagnosticHandAt=0;
        for(int i=0;i<3;i++){
            learning[i].newRound();
            persistOpponentLearning(i);
        }
        played.clear();otherPlayed.clear();waitingCards.clear();cardMoves.clear();countDrops.clear();
        handCards.clear();myPlayed.clear();handPending="";handPendingFrames=0;
        handEverVerified=false;
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
        resultLikeNoHand=false;handEndVisibility.clear();
        baitSignals.resetRound();adviceReentry.reset();adviceRetryGate.reset();
        confirmedPassLeadCleared="";lastLoggedThreatSignature="";
        threatEpoch++;lastThreatScan=0;threatDirty=true;
        for(List<ObservedPass> l:confirmedPasses)l.clear();
        Arrays.fill(threatLabels,"3ĐT — · 4ĐT — · TQ —");
        redealDetector.clear();
        getSharedPreferences("tracker_strategy",MODE_PRIVATE).edit().remove("hand").remove("table").remove("hand_source").apply();
        mainHandler.post(()->{
            if(suggestionPanel!=null)suggestionPanel.setVisibility(View.GONE);
        });
    }
    private void manualReset(){
        setScanStatus("reset · quét mới");
        synchronized(stateLock){
            resetRoundInternal("manual_reset");
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
    /** Recompute structural prevalence outside the expensive frame-decoding loop.
     * This describes unverified sampled worlds, not a real-world chop probability.
     */
    private void scheduleThreatSnapshot(){
        // Keep at most one expensive hypothetical-deal simulation in flight.
        // A later changed table is marked dirty again instead of queuing an
        // unbounded backlog of already-obsolete threat computations.
        if(!threatTaskRunning.compareAndSet(false,true)){
            synchronized(stateLock){threatDirty=true;}
            return;
        }
        final LinkedHashSet<String> mine,seen;
        final int[] counts;
        final List<List<List<String>>> history=new ArrayList<>();
        final List<List<ObservedPass>> passHistory=new ArrayList<>();
        final int version;
        final boolean handTrusted;
        synchronized(stateLock){
            mine=new LinkedHashSet<>(handCards);
            seen=observedCards();counts=knownCounts.clone();
            handTrusted=handEverVerified && !mine.isEmpty() && Collections.disjoint(mine,seen)
                && ObservedHandReconciler.inspect(mine,seen,counts).feasible;
            for(LearnedOpponentHistory h:learning)history.add(h.snapshot());
            for(List<ObservedPass> events:confirmedPasses)passHistory.add(new ArrayList<>(events));
            version=++threatEpoch;
        }
        threatExecutor.execute(()->{
            ThreatScenarioSampler.Seat[] estimate=null;
            try{if(handTrusted)estimate=ThreatScenarioSampler.sample(mine,seen,counts,history,passHistory,
                Objects.hash(mine,seen,Arrays.hashCode(counts),passHistory.size()));}
            catch(Exception error){android.util.Log.w("Tracker","Threat sample unavailable",error);}
            finally{threatTaskRunning.set(false);}
            final ThreatScenarioSampler.Seat[] values=estimate;
            mainHandler.post(()->{
                synchronized(stateLock){
                    if(stopping || version!=threatEpoch)return;
                    StringBuilder nextSignature=new StringBuilder(Arrays.toString(counts));
                    if(values!=null)for(int k=0;k<3;k++){
                        for(int j=0;j<3;j++)nextSignature.append('|').append(values[k].percent(j));
                    }
                    boolean newState=!nextSignature.toString().equals(lastLoggedThreatSignature);
                    if(newState)lastLoggedThreatSignature=nextSignature.toString();
                    for(int k=0;k<3;k++){
                        threatLabels[k]=values==null?"3ĐT — · 4ĐT — · TQ —":values[k].concise();
                        if(values!=null && newState){
                            // Predictions are not ground-truth observations. Avoid
                            // flooding the bounded training log with repeated
                            // Monte Carlo estimates for the identical table.
                            trainEvent("threat_sample","seat",k,
                                "sample_3_pair_run_percent",values[k].percent(0),
                                "sample_4_pair_run_percent",values[k].percent(1),
                                "sample_quartet_percent",values[k].percent(2),
                                "sample_count",values[k].samples,
                                "opponent_remaining",counts[k],
                                "confidence",values[k].lowConfidence?"weak_prior":"model_only_not_calibrated");
                        }
                    }
                }
                refresh();
            });
        });
    }
    private void createPanel(){
        if(wm==null)return;
        for(int i=0;i<3;i++){
            LinearLayout layout=newPanel();
            playerRows[i]=newCardText(11f);
            layout.addView(playerRows[i]);
            threatRows[i]=newCardText(9f);
            threatRows[i].setTextColor(0xffc4e6f5);
            threatRows[i].setVisibility(View.GONE);
            layout.addView(threatRows[i]);
            // Show compact combination estimates even before anyone has played.
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
        scanBadge.setTextSize(10.5f);
        scanBadge.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        scanBadge.setPadding(dp(4),0,dp(4),0);
        // No rounded panel/border: just one short, legible line.
        scanBadge.setBackgroundColor(0x44212b35);
        scanBadge.setSingleLine(true);
        scanBadge.setEllipsize(android.text.TextUtils.TruncateAt.END);
        scanBadgeParams=new WindowManager.LayoutParams(dp(182),dp(31),
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
            mainHandler.post(()->{if(scanBadge!=null)scanBadge.setText("Quét: "+ScanStatus.compact(value));});
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
    private static String boardSignature(Collection<String> cards){
        ArrayList<String> sorted=new ArrayList<>(cards);
        Collections.sort(sorted);
        return String.join(" ",sorted);
    }
    private void updateBoard(Collection<String> latest){
        ArrayList<String> cards=new ArrayList<>(latest);
        Collections.sort(cards);
        String key=boardSignature(cards);
        if(key.equals(boardCandidate))boardFrames++;
        else{boardCandidate=key;boardFrames=1;boardStable=false;}
        if(boardFrames>=2){
            currentBoard=cards;boardStable=true;
            if(!key.equals(confirmedPassLeadCleared))confirmedPassLeadCleared="";
        }
    }
    /** Only a complete and stable visible fan may repair a stale tracked hand.
     * The previous full 13-card scan remains the default way to initialize a deal.
     */
    private boolean updateVisibleHand(HandRecognizer.Result r){
        if(manualHandOverride)return false;
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
        if(handCards.isEmpty() && !HandSyncPolicy.acceptFirstVisualHand(scanned.size(),
            sawAnyLowerCounter || !played.isEmpty() || !myPlayed.isEmpty() || !cardMoves.isEmpty(),
            knownCounts))return false;
        // The local player may open with a straight while all opponents still
        // display 13. Our PREVIOUS verified hand and the actual public cards
        // jointly prove that those missing cards were OUR play. Never attribute
        // an unverified hand-size change to a player just from an OCR gap.
        List<String> localCards=LocalPlayEvidence.confirmed(handCards,scanned,observedCards());
        if(localCards.isEmpty() && handCards.isEmpty() && !handEverVerified &&
                boardStable && played.isEmpty() && countDrops.isEmpty()){
            localCards=LocalPlayEvidence.opening(scanned,currentBoard,knownCounts);
        }
        if(!localCards.isEmpty()){
            for(String c:localCards){
                played.add(c);myPlayed.add(c);
                waitingCards.remove(c);otherPlayed.remove(c);otherHistory.remove(c);
                for(int seat=0;seat<3;seat++){
                    byPlayer[seat].remove(c);playerHistory[seat].remove(c);
                    if(learning[seat].retractCurrentRound(Collections.singleton(c))>0)
                        persistOpponentLearning(seat);
                }
            }
            for(int i=cardMoves.size()-1;i>=0;i--){
                cardMoves.get(i).cards.removeAll(localCards);
                if(cardMoves.get(i).cards.isEmpty())cardMoves.remove(i);
            }
            diag("local_move_inferred","cards",localCards,"reason","verified_fan_and_board");
            trainEvent("local_play_observed","cards",localCards,
                "confidence","verified_fan_shrink_and_public_cards");
        }
        // Three identical, complete visible fan scans outweigh a stale card
        // record. A card cannot be simultaneously in our hand and played.
        // Repair only the overlapping card identities; never fabricate others.
        ObservedHandReconciler.Result check=ObservedHandReconciler.inspect(
            scanned,observedCards(),knownCounts);
        if(!check.feasible){
            // A repeatedly verified visible hand has more direct evidence than
            // stale PUBLIC card classification. Keep showing it but suppress
            // opponent-threat percentages and advice until the deck reconciles.
            // The diagnostic log records this as NOT safe training ground truth.
            diag("hand_deck_conflict","cards",new ArrayList<>(scanned),
                "counts",knownCounts.clone(),"seen",new ArrayList<>(observedCards()),
                "reason","public_history_overcounts_deck");
        }
        LinkedHashSet<String> contradictions=new LinkedHashSet<>(check.contradictions);
        if(!contradictions.isEmpty())diag("hand_conflict","cards",new ArrayList<>(contradictions),
            "hand",new ArrayList<>(scanned),"counts",knownCounts.clone());
        if(!contradictions.isEmpty()){
            trainEvent("hand_history_conflict","cards",new ArrayList<>(contradictions),
                "reason","stable_visible_hand_overlaps_observed_history",
                "confidence","correction_not_training_ground_truth");
            for(String c:contradictions){
                played.remove(c);waitingCards.remove(c);
                myPlayed.remove(c);otherPlayed.remove(c);otherHistory.remove(c);
                for(int seat=0;seat<3;seat++){
                    byPlayer[seat].remove(c);playerHistory[seat].remove(c);
                    if(learning[seat].retractCurrentRound(Collections.singleton(c))>0){
                        diag("learned_move_retracted","seat",seat,"cards",Collections.singletonList(c));
                        persistOpponentLearning(seat);
                    }
                }
            }
            for(int i=cardMoves.size()-1;i>=0;i--){
                cardMoves.get(i).cards.removeAll(contradictions);
                if(cardMoves.get(i).cards.isEmpty())cardMoves.remove(i);
            }
            recentCards.clear();
            threatDirty=true;threatEpoch++;
        }
        // The current visual hand remains usable even if a winner's counter is
        // hidden (-1). Sampling of opponents still checks its own requirements.
        // Visually confirmed cards can correct stale historical classification,
        // but gaps in earlier plays remain unknown rather than invented.
        handCards.clear();handCards.addAll(scanned);
        handEverVerified=true;
        diag("hand_synced","cards",new ArrayList<>(scanned),"counts",knownCounts.clone(),
            "reason",!check.feasible?"stable_visual_conflicting_ledger":
                contradictions.isEmpty()?"stable_visual":"visual_corrected_history");
        manualHandOverride=false;
        threatDirty=true;threatEpoch++;lastThreatScan=0;
        trainEvent(check.feasible?"hand_verified":"hand_conflict_pending",
            "cards",new ArrayList<>(scanned),"method","visible_hand_after_play",
            "confidence",!check.feasible?"requires_ledger_review":
                contradictions.isEmpty()?"multi_frame_verified":"multi_frame_history_repaired",
            "count",scanned.size());
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
    /** Stable current inputs for automatic *recovery* after a blocked advice. */
    private String adviceFingerprint(){
        return boardStable+"|"+handEverVerified+"|"+handCards.hashCode()+"|"+
            observedCards().hashCode()+"|"+currentBoard.hashCode()+"|"+
            Arrays.toString(knownCounts);
    }
    /** Snapshot and analyze off the screen-capture thread. Never block new video frames. */
    private void runFreshAdvice(){
        final LinkedHashSet<String> hand,seen;
        final List<String> lead;
        final int[] counts;
        final List<List<List<String>>> histories=new ArrayList<>();
        final boolean turn,boardOK,handOK,countsKnown,consistent,overlap;
        final int epoch,request;
        synchronized(stateLock){
            turn=myTurn;
            boardOK=boardStable;
            hand=new LinkedHashSet<>(handCards);
            seen=observedCards();
            // Three explicitly confirmed passes over our own latest lead
            // create a fresh opening even if the renderer leaves old cards up.
            boolean openingByPass=boardStable && !currentBoard.isEmpty() &&
                confirmedPassLeadCleared.equals(boardSignature(currentBoard));
            lead=openingByPass?new ArrayList<>():new ArrayList<>(currentBoard);
            counts=knownCounts.clone();
            for(LearnedOpponentHistory h:learning)histories.add(h.snapshot());
            PartialDeckState.Result check=PartialDeckState.check(hand,seen,counts);
            consistent=check.feasible && check.missing<=24;
            boolean allCountersKnown=true;
            for(int count:counts)if(count<0||count>13){allCountersKnown=false;break;}
            countsKnown=allCountersKnown;
            // An old partial screenshot should not invalidate a hand already
            // verified earlier this round. Never trust unverified/guessed cards.
            handOK=!hand.isEmpty() && handEverVerified;
            overlap=!Collections.disjoint(hand,seen) || !Collections.disjoint(hand,lead);
            epoch=turnEpoch;
            request=++adviceRequestId;
            adviceRetryGate.requested(adviceFingerprint(),SystemClock.elapsedRealtime());
        }
        adviceExecutor.execute(()->{
            final MonteCarloAdvisor.Advice result;
            if(!turn)result=new MonteCarloAdvisor.Advice("CHƯA ĐẾN LƯỢT","");
            else if(!boardOK)result=new MonteCarloAdvisor.Advice("QUÉT LẠI BÀI GIỮA BÀN","");
            else if(!handOK||overlap)result=new MonteCarloAdvisor.Advice("QUÉT LẠI BÀI TAY","");
            else {
                MonteCarloAdvisor.Advice computed;
                try {
                    StrategyMode mode=StrategyMode.fromKey(getSharedPreferences("tracker_strategy",MODE_PRIVATE)
                        .getString("mode",StrategyMode.PLACEMENT.key));
                    if(consistent){
                        computed=MonteCarloAdvisor.adviseBehaviorAware(hand,lead,seen,counts,histories,mode);
                    }else if(!countsKnown || PartialDeckState.check(hand,seen,counts).feasible){
                        // A missing counter or incomplete history does not erase
                        // the known legal plays. Show a conservative suggestion,
                        // but DO NOT manufacture a blocking percentage.
                        computed=MonteCarloAdvisor.adviseWithIncompleteHistory(hand,lead,seen,mode);
                    }else{
                        computed=new MonteCarloAdvisor.Advice("QUÉT LẠI BÀI VÀ LỊCH SỬ","");
                    }
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
                adviceRetryGate.finished(result.title.startsWith("QUÉT LẠI"));
                trainEvent("advice_proposed","title",result.title,
                    "detail",result.detail,"hand_count",hand.size(),
                    "seen_count",seen.size(),"lead",lead,"opponent_counts",counts,
                    "source","heuristic_monte_carlo_not_calibrated",
                    "result_is_actual_play",false);
                showAdvice(result,"");
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
        // A suggestion must NEVER hide the user's cards or the remaining deck.
        // The popup is anchored at top-left, away from the lower HUD.
        suggestionPanel.setVisibility(View.VISIBLE);
        dismissSuggestion=()->{
            if(suggestionPanel!=null)suggestionPanel.setVisibility(View.GONE);
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
            if(threatRows[i]!=null)threatRows[i].setTextSize(TypedValue.COMPLEX_UNIT_PX,Math.max(13f,side*.015f));
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
            if(i<3)needed=Math.max(desiredWidth(playerRows[i]),desiredWidth(threatRows[i]));
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
                if(playerRows[i]!=null){
                    playerRows[i].setText(recentPlaySpans(playerHistory[i]));
                    playerRows[i].setVisibility(n>0?View.VISIBLE:View.GONE);
                }
                if(threatRows[i]!=null){
                    String baitNote=baitSignals.note(i);
                    threatRows[i].setText(threatLabels[i]+(baitNote.isEmpty()?"":"\n"+baitNote));
                    threatRows[i].setVisibility(threatLabels[i].isEmpty()?View.GONE:View.VISIBLE);
                }
                if(p!=null)p.setVisibility(n>0||!threatLabels[i].isEmpty()?View.VISIBLE:View.GONE);
            }
            if(resultLikeNoHand){
                handRow.setVisibility(View.VISIBLE);
                handRow.setText("Tôi: không thấy bài · chờ ván mới (chưa xác nhận kết quả)");
            }else if(handCards.isEmpty())handRow.setVisibility(View.GONE);
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
            // Every red/green suit must be outside my VERIFIED hand and not public.
            // "Chưa đánh" itself still includes my own cards, by definition.
            boolean cleanVisualHand=handEverVerified && !handCards.isEmpty() &&
                ObservedHandReconciler.inspect(handCards,observedCards(),knownCounts).feasible;
            OpponentCandidatePool.Result pool=OpponentCandidatePool.analyze(
                remaining,handCards,cleanVisualHand,knownCounts);
            remainingRow.setText(resultLikeNoHand?
                "Chưa đánh: tạm dừng đối soát khi không thấy bài · giữ lịch sử để xuất log":
                buildRemainingSpans(remaining,pool));
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
    /** Color only the *suits* opponents could still hold, never a whole rank
     * that includes one of my own cards. Red: all 4 suits are outside my hand.
     * Green: this rank can belong to a consecutive-pairs run in the shared
     * possible pool. Gray: suit is in my verified hand. Such colors do not
     * establish that all needed cards belong to one particular opponent.
     */
    private CharSequence buildRemainingSpans(Collection<String> remaining,OpponentCandidatePool.Result pool){
        SpannableStringBuilder sb=new SpannableStringBuilder();
        // The unplayed deck includes OUR cards. Dim owned cards rather than
        // removing them: the bright remaining suits are the candidates that
        // opponents may hold, subject to the history's reliability.
        sb.append("Chưa đánh (").append(String.valueOf(remaining.size())).append(") · ");
        int summaryStart=sb.length();
        sb.append(pool.compactCounts()).append("  ");
        int legendStart=sb.length();
        if(!pool.mine.isEmpty()) {
            sb.append("[mờ: tay tôi]  ");
            sb.setSpan(new ForegroundColorSpan(0xff8999ad),legendStart,sb.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        if(pool.reliable)sb.setSpan(new ForegroundColorSpan(0xffabd7ee),summaryStart,sb.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        else sb.setSpan(new ForegroundColorSpan(0xffffc07e),summaryStart,sb.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        HashSet<String> unplayed=new HashSet<>(remaining);
        String[] ranks=CardWarnings.RANKS;
        String[] suits={"S","C","D","H"};
        for(int i=0;i<ranks.length;i++){
            String rank=ranks[i];
            boolean exists=false;
            for(String suit:suits)if(unplayed.contains(rank+suit)){exists=true;break;}
            if(!exists)continue;
            int rankStart=sb.length();
            sb.append(rank);
            boolean allMine=true;
            for(String suit:suits)if(unplayed.contains(rank+suit) && !pool.mine.contains(rank+suit))allMine=false;
            if(allMine){
                sb.setSpan(new ForegroundColorSpan(0x88b7c4d3),rankStart,sb.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                sb.setSpan(new RelativeSizeSpan(.93f),rankStart,sb.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            for(String suit:suits){
                String card=rank+suit;
                if(!unplayed.contains(card))continue;
                int a=sb.length();
                sb.append(suitSymbol(suit));
                int b=sb.length();
                OpponentCandidatePool.Cue cue=pool.cue(card);
                if(cue==OpponentCandidatePool.Cue.MINE){
                    // Owned cards remain in "Chưa đánh", visually de-emphasized.
                    sb.setSpan(new ForegroundColorSpan(0x88b7c4d3),a,b,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    sb.setSpan(new RelativeSizeSpan(.92f),a,b,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }else if(cue==OpponentCandidatePool.Cue.POSSIBLE_QUARTET){
                    sb.setSpan(new ForegroundColorSpan(0xffff8585),a,b,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    sb.setSpan(new BackgroundColorSpan(0xff49242c),a,b,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    sb.setSpan(new StyleSpan(android.graphics.Typeface.BOLD),a,b,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }else if(cue==OpponentCandidatePool.Cue.POSSIBLE_4_PAIRS ||
                         cue==OpponentCandidatePool.Cue.POSSIBLE_3_PAIRS){
                    sb.setSpan(new ForegroundColorSpan(0xffa5ffbd),a,b,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    sb.setSpan(new BackgroundColorSpan(0xff194b35),a,b,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    sb.setSpan(new StyleSpan(android.graphics.Typeface.BOLD),a,b,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    if(cue==OpponentCandidatePool.Cue.POSSIBLE_4_PAIRS)
                        sb.setSpan(new UnderlineSpan(),a,b,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
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
        Set<String> alreadySeen=observedCards();
        for(String r:ranks)for(String s:suits){
            String c=r+s;
            if(!alreadySeen.contains(c) || (handEverVerified && handCards.contains(c)))remain.add(c);
        }
        return remain;
    }
    @Override public void onDestroy(){
        trainEvent("capture_stopped","result","unknown");
        stopping=true;
        mainHandler.removeCallbacks(captureWatchdog);
        adviceExecutor.shutdownNow();
        threatExecutor.shutdownNow();
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
