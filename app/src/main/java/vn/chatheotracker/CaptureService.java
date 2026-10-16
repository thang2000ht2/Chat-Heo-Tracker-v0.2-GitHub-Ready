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
    static final String EXTRA_RESULT="projection_result",EXTRA_DATA="projection_data",EXTRA_CARD="card";
    private MediaProjection projection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;
    private HandlerThread worker;
    private Handler workerHandler, mainHandler;
    private WindowManager wm;
    private View panel;
    private TextView summary;
    private CardRecognizer recognizer;
    private long lastScanned=0;
    private String pending="";private int pendingFrames=0;
    private final LinkedHashSet<String> played=new LinkedHashSet<>();
    private boolean stopping=false;
    private int densityDpi;

    @Override public void onCreate(){super.onCreate();mainHandler=new Handler(Looper.getMainLooper());}
    @Override public int onStartCommand(Intent intent,int flags,int startId){
        if(intent==null)return START_NOT_STICKY;
        String action=intent.getAction();
        if(ACTION_STOP.equals(action)){stopSelf();return START_NOT_STICKY;}
        if(ACTION_RESET.equals(action)){played.clear();pending="";pendingFrames=0;refresh();return START_NOT_STICKY;}
        if(ACTION_ADD.equals(action)||ACTION_REMOVE.equals(action)) {
            String c=intent.getStringExtra(EXTRA_CARD);
            if(c!=null&&!c.isEmpty()) {
                if(ACTION_ADD.equals(action))played.add(c); else played.remove(c);
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
            } catch(Exception err){android.util.Log.e("Tracker","Start error",err);stopSelf();}
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
            List<String> cards=recognizer.recognize(cropped);cropped.recycle();
            String newSignature=String.join(",",cards);
            // Require stable recognition for 2 consecutive sampled frames.
            if(!newSignature.isEmpty() && cards.size()>0){
                if(newSignature.equals(pending))pendingFrames++;else{pending=newSignature;pendingFrames=1;}
                if(pendingFrames>=2){
                    boolean changed=false;
                    for(String c:cards)if(played.add(c))changed=true;
                    if(changed)mainHandler.post(this::refresh);
                }
            }else{pending="";pendingFrames=0;}
        }catch(Exception e){android.util.Log.w("Tracker","Frame scan error",e);}
        finally {if(image!=null)image.close();}
    }
    private void createPanel(){
        LinearLayout wrap=new LinearLayout(this);wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.setPadding(14,9,14,9);
        android.graphics.drawable.GradientDrawable bg=new android.graphics.drawable.GradientDrawable();
        bg.setColor(0xeb171b27);bg.setCornerRadius(20);bg.setStroke(2,0xff707780);
        wrap.setBackground(bg);
        TextView header=new TextView(this);header.setText("☰  ĐẾM BÀI  •  Kéo để di chuyển");
        header.setTextColor(0xffeeeeee);header.setTextSize(12);wrap.addView(header);
        summary=new TextView(this);summary.setTextColor(0xffffffff);summary.setTextSize(14);
        summary.setMaxLines(5);wrap.addView(summary);
        LinearLayout actions=new LinearLayout(this);
        Button reset=new Button(this);reset.setText("Ván mới");reset.setAllCaps(false);reset.setTextSize(12);
        reset.setOnClickListener(v->{played.clear();pending="";pendingFrames=0;refresh();});
        Button hide=new Button(this);hide.setText("Ẩn");hide.setTextSize(12);hide.setAllCaps(false);
        hide.setOnClickListener(v->{boolean show=summary.getVisibility()==View.GONE;
            summary.setVisibility(show?View.VISIBLE:View.GONE);
            hide.setText(show?"Ẩn":"Hiện");});
        actions.addView(reset);actions.addView(hide);wrap.addView(actions);
        int type=WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        WindowManager.LayoutParams p=new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,WindowManager.LayoutParams.WRAP_CONTENT,type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT);
        p.gravity=Gravity.TOP|Gravity.LEFT;p.x=25;p.y=250;
        header.setOnTouchListener(new View.OnTouchListener(){int initialX,initialY;float downX,downY;
            @Override public boolean onTouch(View v,android.view.MotionEvent e){
                if(e.getAction()==android.view.MotionEvent.ACTION_DOWN){
                    initialX=p.x;initialY=p.y;downX=e.getRawX();downY=e.getRawY();return true;
                }
                if(e.getAction()==android.view.MotionEvent.ACTION_MOVE){
                    p.x=initialX+(int)(e.getRawX()-downX);
                    p.y=initialY+(int)(e.getRawY()-downY);
                    wm.updateViewLayout(wrap,p);return true;
                }
                return e.getAction()==android.view.MotionEvent.ACTION_UP;
            }
        });
        panel=wrap;wm.addView(panel,p);refresh();
    }
    private void refresh(){
        if(summary==null)return;
        StringBuilder t=new StringBuilder("Đã đánh: ").append(played.size()).append("/52")
            .append("  •  Chưa xuất hiện: ").append(52-played.size());
        if(!played.isEmpty()) {
            t.append("\n");int n=0;
            for(String c:played){if(n++>=22){t.append(" …");break;}t.append(pretty(c)).append("  ");}
        }else t.append("\nChưa ghi nhận lá nào");
        summary.setText(t.toString());
    }
    private static String pretty(String s){
        if(s.isEmpty())return s;
        String rank=s.substring(0,s.length()-1),suit=s.substring(s.length()-1);
        String symbol=suit.equals("S")?"♠":suit.equals("C")?"♣":suit.equals("H")?"♥":"♦";
        return rank+symbol;
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
        panel=null;summary=null;
        super.onDestroy();
    }
    @Override public android.os.IBinder onBind(Intent i){return null;}
}
