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
    private View panel;
    private final TextView[] rowViews=new TextView[5];
    private TextView status;
    private CardRecognizer recognizer;
    private CountRecognizer countRecognizer;
    private final Object stateLock=new Object();
    private long lastScanned=0;
    private String pending="";private int pendingFrames=0;
    private final LinkedHashSet<String> played=new LinkedHashSet<>();
    private final LinkedHashSet<String>[] byPlayer=new LinkedHashSet[]{new LinkedHashSet<>(),new LinkedHashSet<>(),new LinkedHashSet<>()};
    private final LinkedHashSet<String> otherPlayed=new LinkedHashSet<>();
    private final LinkedHashSet<String> waitingCards=new LinkedHashSet<>();
    private final ArrayList<CardMove> cardMoves=new ArrayList<>();
    private final ArrayList<CountDrop> countDrops=new ArrayList<>();
    private final int[] knownCounts={-1,-1,-1},lastCandidates={-1,-1,-1},candidateFrames={0,0,0};
    private final int[] missingCards={0,0,0};
    private boolean sawAnyLowerCounter=false;
    private boolean stopping=false;
    private int densityDpi;

    @Override public void onCreate(){super.onCreate();mainHandler=new Handler(Looper.getMainLooper());}
    @Override public int onStartCommand(Intent intent,int flags,int startId){
        if(intent==null)return START_NOT_STICKY;
        String action=intent.getAction();
        if(ACTION_STOP.equals(action)){stopSelf();return START_NOT_STICKY;}
        if(ACTION_RESET.equals(action)){
            synchronized(stateLock){resetRoundInternal();}
            refresh();return START_NOT_STICKY;
        }
        if(ACTION_ADD.equals(action)||ACTION_REMOVE.equals(action)){
            String c=intent.getStringExtra(EXTRA_CARD);
            if(c!=null&&!c.isEmpty())synchronized(stateLock){
                if(ACTION_ADD.equals(action)){
                    if(played.add(c))otherPlayed.add(c);
                }else{
                    played.remove(c);otherPlayed.remove(c);waitingCards.remove(c);
                    for(LinkedHashSet<String> set:byPlayer)set.remove(c);
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
                mainHandler.post(() -> resizeCapture(width,height));
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
            if(now-lastScanned<300)return;
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
        if(sawAnyLowerCounter && allThirteen()){
            resetRoundInternal();
            for(int i=0;i<3;i++){
                knownCounts[i]=13;lastCandidates[i]=13;candidateFrames[i]=2;
            }
            return true;
        }
        String signature=String.join(",",visible);
        if(!signature.isEmpty()){
            if(signature.equals(pending))pendingFrames++;
            else {pending=signature;pendingFrames=1;}
            if(pendingFrames>=2){
                LinkedHashSet<String> newlySeen=new LinkedHashSet<>();
                for(String c:visible)if(!played.contains(c)&&!waitingCards.contains(c))newlySeen.add(c);
                if(!newlySeen.isEmpty()){
                    waitingCards.addAll(newlySeen);
                    // A multi-card play can materialize across adjacent captured frames.
                    if(!cardMoves.isEmpty() && now-cardMoves.get(cardMoves.size()-1).time<650){
                        cardMoves.get(cardMoves.size()-1).cards.addAll(newlySeen);
                        cardMoves.get(cardMoves.size()-1).updated=now;
                    }else cardMoves.add(new CardMove(newlySeen,now));
                    changed=true;
                }
            }
        }else {pending="";pendingFrames=0;}
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
                for(String card:move.cards)if(played.add(card))byPlayer[drop.player].add(card);
                if(drop.delta>move.cards.size())missingCards[drop.player]+=drop.delta-move.cards.size();
                waitingCards.removeAll(move.cards);
                cardMoves.remove(m);
                changed=true;
            }else if(now-move.time>2600){
                // This could be the user's own turn; without a visible count change,
                // the app must NOT invent an opponent attribution.
                for(String card:move.cards)if(played.add(card))otherPlayed.add(card);
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
        Arrays.fill(knownCounts,-1);Arrays.fill(lastCandidates,-1);
        Arrays.fill(candidateFrames,0);Arrays.fill(missingCards,0);
        pending="";pendingFrames=0;sawAnyLowerCounter=false;
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
    private TextView addCardRow(LinearLayout parent,String label){
        LinearLayout line=new LinearLayout(this);line.setOrientation(LinearLayout.HORIZONTAL);
        line.setGravity(Gravity.CENTER_VERTICAL);
        TextView caption=new TextView(this);caption.setText(label);
        caption.setTextColor(0xffffdfa1);caption.setTextSize(10);
        caption.setMinWidth(dp(67));
        line.addView(caption);
        HorizontalScrollView scroller=new HorizontalScrollView(this);
        scroller.setHorizontalScrollBarEnabled(false);
        TextView contents=new TextView(this);
        contents.setTextColor(0xffffffff);contents.setTextSize(10);
        contents.setSingleLine(true);
        contents.setPadding(dp(2),dp(1),dp(6),dp(1));
        scroller.addView(contents);
        line.addView(scroller,new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f));
        parent.addView(line);
        return contents;
    }
    private int dp(float x){return (int)(x*getResources().getDisplayMetrics().density+0.5f);}
    private void createPanel(){
        LinearLayout wrap=new LinearLayout(this);wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.setPadding(dp(5),dp(2),dp(5),dp(2));
        android.graphics.drawable.GradientDrawable bg=new android.graphics.drawable.GradientDrawable();
        bg.setColor(0xf2171b27);bg.setCornerRadius(dp(8));bg.setStroke(dp(1),0xff707780);
        wrap.setBackground(bg);
        LinearLayout toolbar=new LinearLayout(this);toolbar.setOrientation(LinearLayout.HORIZONTAL);
        TextView header=new TextView(this);
        header.setText("☰  ĐẾM BÀI  ");
        header.setTextColor(0xffeeeeee);header.setTextSize(10);
        toolbar.addView(header,new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f));
        TextView reset=new TextView(this);reset.setText("Ván mới   ");reset.setTextSize(10);
        reset.setTextColor(0xffffdfa1);reset.setOnClickListener(v->{
            synchronized(stateLock){resetRoundInternal();}refresh();
        });toolbar.addView(reset);
        TextView hide=new TextView(this);hide.setText("Thu gọn");hide.setTextSize(10);
        hide.setTextColor(0xffffdfa1);toolbar.addView(hide);wrap.addView(toolbar);
        status=new TextView(this);status.setTextColor(0xfff9dd76);status.setTextSize(9);
        wrap.addView(status);
        rowViews[0]=addCardRow(wrap,"P1 (Trên)");
        rowViews[1]=addCardRow(wrap,"P2 (Trái)");
        rowViews[2]=addCardRow(wrap,"P3 (Phải)");
        rowViews[3]=addCardRow(wrap,"Bạn/khác");
        rowViews[4]=addCardRow(wrap,"Chưa đánh");
        hide.setOnClickListener(v->{
            View firstRow=(View)rowViews[0].getParent().getParent();
            boolean show=firstRow.getVisibility()==View.GONE;
            for(TextView row:rowViews){
                View line=(View)row.getParent().getParent();line.setVisibility(show?View.VISIBLE:View.GONE);
            }
            status.setVisibility(show?View.VISIBLE:View.GONE);
            hide.setText(show?"Thu gọn":"Mở rộng");
        });
        int type=WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        android.util.DisplayMetrics metrics=new android.util.DisplayMetrics();
        wm.getDefaultDisplay().getRealMetrics(metrics);
        int width=Math.min((int)(metrics.widthPixels*0.53f),dp(480));
        WindowManager.LayoutParams p=new WindowManager.LayoutParams(width,WindowManager.LayoutParams.WRAP_CONTENT,type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT);
        p.gravity=Gravity.TOP|Gravity.LEFT;p.x=dp(8);p.y=dp(8);
        header.setOnTouchListener(new View.OnTouchListener(){int initialX,initialY;float downX,downY;
            @Override public boolean onTouch(View v,MotionEvent e){
                if(e.getAction()==MotionEvent.ACTION_DOWN){
                    initialX=p.x;initialY=p.y;downX=e.getRawX();downY=e.getRawY();return true;
                }
                if(e.getAction()==MotionEvent.ACTION_MOVE){
                    p.x=initialX+(int)(e.getRawX()-downX);
                    p.y=initialY+(int)(e.getRawY()-downY);
                    wm.updateViewLayout(wrap,p);return true;
                }
                return e.getAction()==MotionEvent.ACTION_UP;
            }
        });
        panel=wrap;wm.addView(panel,p);refresh();
    }
    private void refresh(){
        if(status==null)return;
        synchronized(stateLock){
            StringBuilder st=new StringBuilder("Đã đánh: ").append(played.size()).append("/52");
            st.append("  |  Đang đối chiếu: ").append(waitingCards.size());
            int missing=missingCards[0]+missingCards[1]+missingCards[2];
            if(missing>0)st.append("  |  Có thể bỏ sót ").append(missing).append(" lá");
            status.setText(st.toString());
            for(int i=0;i<3;i++)rowViews[i].setText(renderSet(byPlayer[i]));
            rowViews[3].setText(renderSet(otherPlayed));
            rowViews[4].setText("("+(52-played.size())+") "+renderSet(remainingCards()));
        }
    }
    private static String pretty(String s){
        if(s.isEmpty())return s;
        String rank=s.substring(0,s.length()-1),suit=s.substring(s.length()-1);
        String symbol=suit.equals("S")?"♠":suit.equals("C")?"♣":suit.equals("H")?"♥":"♦";
        return rank+symbol;
    }
    private static String renderSet(Collection<String> cards){
        List<String> sorted=new ArrayList<>(cards);
        sorted.sort(CaptureService::compareCards);
        if(sorted.isEmpty())return "-";
        StringBuilder sb=new StringBuilder();
        for(int i=0;i<sorted.size();i++){
            if(i>0)sb.append("  ");
            sb.append(pretty(sorted.get(i)));
        }
        return sb.toString();
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
    private static int compareCards(String a,String b){
        int ra=rankValue(a),rb=rankValue(b);
        if(ra!=rb)return Integer.compare(ra,rb);
        return Integer.compare(suitValue(a),suitValue(b));
    }
    private static int rankValue(String c){
        String r=c.substring(0,c.length()-1);
        switch(r){
            case "3": return 0; case "4": return 1; case "5": return 2; case "6": return 3;
            case "7": return 4; case "8": return 5; case "9": return 6; case "10": return 7;
            case "J": return 8; case "Q": return 9; case "K": return 10; case "A": return 11;
            case "2": return 12; default: return 99;
        }
    }
    private static int suitValue(String c){
        String s=c.substring(c.length()-1);
        switch(s){
            case "S": return 0; case "C": return 1; case "D": return 2; case "H": return 3; default: return 9;
        }
    }
    @Override public void onDestroy(){
        stopping=true;
        if(imageReader!=null){imageReader.setOnImageAvailableListener(null,null);}
        if(virtualDisplay!=null){virtualDisplay.release();virtualDisplay=null;}
        if(imageReader!=null){imageReader.close();imageReader=null;}
        MediaProjection old=projection;projection=null;
        if(old!=null)try{old.stop();}catch(Exception ignored){}
        if(worker!=null){worker.quitSafely();worker=null;}
        if(panel!=null && wm!=null)try{wm.removeView(panel);}catch(Exception ignored){}
        panel=null;status=null;
        super.onDestroy();
    }
    @Override public android.os.IBinder onBind(Intent i){return null;}
}
