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

/** Local-only capture & overlay service. Does not connect to game process or network. */
public final class CaptureService extends Service {
    static final String ACTION_START="vn.chatheotracker.START",ACTION_STOP="vn.chatheotracker.STOP";
    static final String ACTION_RESET="vn.chatheotracker.RESET";
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
    private TextView otherRow, remainingRow, status;
    // Only this small top-left window accepts touches. Card HUD windows are pass-through.
    private TextView resetButton, suggestButton, suggestionText;
    private WindowManager.LayoutParams resetButtonParams, suggestButtonParams, suggestionPanelParams;
    private View suggestionPanel;
    private Runnable dismissSuggestion;
    private CardRecognizer recognizer;
    private CountRecognizer countRecognizer;
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
        countRecognizer=new CountRecognizer(this);
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
    private void onImage(ImageReader reader){
        Image image=null;
        try {
            image=reader.acquireLatestImage();
            if(image==null)return;
            long now=SystemClock.elapsedRealtime();
            if(now-lastScanned<240)return;
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
            int[] counts=countRecognizer.read(normalized);
            normalized.recycle();
            boolean changed;
            synchronized(stateLock){changed=handleFrame(cards,counts,now);}
            if(changed)mainHandler.post(this::refresh);
        }catch(Exception e){android.util.Log.w("Tracker","Frame scan error",e);}
        finally {if(image!=null)image.close();}
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
    private void resetRoundInternal(){
        played.clear();otherPlayed.clear();waitingCards.clear();cardMoves.clear();countDrops.clear();
        for(LinkedHashSet<String> cards:byPlayer)cards.clear();
        for(RecentMoves history:playerHistory)history.clear();
        otherHistory.clear();
        Arrays.fill(knownCounts,-1);Arrays.fill(lastCandidates,-1);
        Arrays.fill(candidateFrames,0);Arrays.fill(missingCards,0);
        recentCards.clear();sawAnyLowerCounter=false;
        newDealStableFrames=0;
        getSharedPreferences("tracker_strategy",MODE_PRIVATE).edit().remove("hand").remove("table").apply();
        mainHandler.post(()->{
            if(suggestionPanel!=null)suggestionPanel.setVisibility(View.GONE);
        });
    }
    private void manualReset(){
        synchronized(stateLock){
            resetRoundInternal();
            ignoreCardsUntil=SystemClock.elapsedRealtime()+900;
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
        otherRow=newCardText(10f);
        bottom.addView(otherRow);
        remainingRow=newCardText(10f);
        bottom.addView(remainingRow);
        status=newCaption("");
        bottom.addView(status);
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
            params.alpha=.90f;
            panelParams[i]=params;
            wm.addView(panels[i],params);
        }
        createResetButton();
        createSuggestControls();
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
    /** Small, opt-in suggestion button. It never taps or controls the game. */
    private void createSuggestControls(){
        suggestButton=new TextView(this);
        suggestButton.setText("Gợi ý");
        suggestButton.setContentDescription("Bấm để xem gợi ý chiến thuật");
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
        suggestionPanelParams.alpha=.96f;
        wm.addView(suggestionPanel,suggestionPanelParams);
        placeSuggestionControls();
        wm.updateViewLayout(suggestionPanel,suggestionPanelParams);
    }
    private void onSuggestClick(){
        if(suggestionText==null||suggestionPanel==null)return;
        String raw=getSharedPreferences("tracker_strategy",MODE_PRIVATE).getString("hand", "");
        String tableRaw=getSharedPreferences("tracker_strategy",MODE_PRIVATE).getString("table", "");
        StrategyAdvisor.Input parsed=StrategyAdvisor.parse(raw);
        if(parsed.error!=null){
            showAdvice(new MonteCarloAdvisor.Advice("Thiếu bài tay", "Mở Tracker để nhập các lá hiện còn trên tay."));
            return;
        }
        StrategyAdvisor.Input table=tableRaw.trim().isEmpty()?null:StrategyAdvisor.parse(tableRaw);
        if(table!=null&&table.error!=null){
            showAdvice(new MonteCarloAdvisor.Advice("Sai bài cần chặn", "Mở Tracker để sửa ký hiệu bộ bài cần chặn."));
            return;
        }
        final LinkedHashSet<String> actualHand=new LinkedHashSet<>(parsed.cards);
        final LinkedHashSet<String> snapshotPlayed;
        final int[] counts;
        synchronized(stateLock){
            snapshotPlayed=new LinkedHashSet<>(played);
            counts=knownCounts.clone();
        }
        final Collection<String> lead=table==null?Collections.emptyList():new ArrayList<>(table.cards);
        if(dismissSuggestion!=null)mainHandler.removeCallbacks(dismissSuggestion);
        suggestionText.setText("Đang phân tích các nước đánh và bài chưa biết…");
        suggestionPanel.setVisibility(View.VISIBLE);
        suggestButton.setEnabled(false);
        // Run the combinatorial enumeration and deal sampling off the UI thread.
        if(workerHandler==null){
            suggestButton.setEnabled(true);
            showAdvice(new MonteCarloAdvisor.Advice("Chưa bật theo dõi", "Mở Tracker và cấp quyền ghi màn hình trước khi xem phân tích."));
            return;
        }
        workerHandler.post(()->{
            MonteCarloAdvisor.Advice advice;
            try{
                advice=MonteCarloAdvisor.advise(actualHand,lead,snapshotPlayed,counts);
            }catch(Exception err){
                android.util.Log.e("Tracker","Advisor failed",err);
                advice=new MonteCarloAdvisor.Advice("Chưa thể phân tích", "Dữ liệu ván hoặc bài tay chưa phù hợp. Hãy kiểm tra lại.");
            }
            final MonteCarloAdvisor.Advice result=advice;
            mainHandler.post(()->{
                if(suggestButton!=null)suggestButton.setEnabled(true);
                showAdvice(result);
            });
        });
    }
    private void showAdvice(MonteCarloAdvisor.Advice advice){
        if(suggestionPanel==null||suggestionText==null)return;
        suggestionText.setText(advice.title+"\n"+advice.detail);
        if(dismissSuggestion!=null)mainHandler.removeCallbacks(dismissSuggestion);
        suggestionPanel.setVisibility(View.VISIBLE);
        dismissSuggestion=()->{if(suggestionPanel!=null)suggestionPanel.setVisibility(View.GONE);};
        mainHandler.postDelayed(dismissSuggestion,16000);
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
            suggestionPanelParams.width=Math.max(dp(170),Math.min(dp(345),(int)(screen*.46f)));
            suggestionPanelParams.x=dp(53);suggestionPanelParams.y=dp(54);
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
    }
    private void sizeOverlayFonts(int w,int h){
        float side=Math.min(w,h);
        for(int i=0;i<3;i++){
            if(playerRows[i]!=null)playerRows[i].setTextSize(TypedValue.COMPLEX_UNIT_PX,Math.max(18f,side*.021f));
        }
        if(status!=null)status.setTextSize(TypedValue.COMPLEX_UNIT_PX,Math.max(13f,side*.015f));
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
            else needed=Math.max(desiredWidth(remainingRow),Math.max(desiredWidth(otherRow),desiredWidth(status)));
            // The reset control occupies its own window, not HUD content.
            p.width=Math.min(maxWidth,Math.max(minWidth,needed+dp(20)));
            p.height=WindowManager.LayoutParams.WRAP_CONTENT;
            if(landscape){
                if(i==0){p.x=(width-p.width)/2;p.y=(int)(height*.215f);}
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
            if(otherPlayed.isEmpty())otherRow.setVisibility(View.GONE);
            else{
                otherRow.setVisibility(View.VISIBLE);
                otherRow.setText(recentPlaySpans(otherHistory, "Khác  "));
            }
            List<String> remaining=remainingCards();
            remainingRow.setText(buildRemainingSpans(remaining));
            int missing=missingCards[0]+missingCards[1]+missingCards[2];
            if(missing>0 || !waitingCards.isEmpty()){
                StringBuilder info=new StringBuilder();
                if(missing>0)info.append("Thiếu nhận diện: ").append(missing);
                if(!waitingCards.isEmpty()){
                    if(info.length()>0)info.append(" · ");
                    info.append("Đang ghép: ").append(waitingCards.size());
                }
                status.setVisibility(View.VISIBLE);
                status.setText(info.toString());
            }else status.setVisibility(View.GONE);
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
    private CharSequence buildRemainingSpans(Collection<String> remaining){
        CardWarnings.Result warnings=CardWarnings.analyze(remaining);
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
            int count=warnings.remainingByRank[i];
            if(count==0)continue;
            int start=sb.length();
            sb.append(rank);
            for(String suit:suits){
                if(unseen.contains(rank+suit))sb.append(suitSymbol(suit));
            }
            if(count==4){
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
        suggestButton=null;suggestButtonParams=null; suggestionText=null;suggestionPanel=null;suggestionPanelParams=null;
        Arrays.fill(playerRows,null);
        otherRow=null;remainingRow=null;status=null;
        super.onDestroy();
    }
    @Override public android.os.IBinder onBind(Intent i){return null;}
}
