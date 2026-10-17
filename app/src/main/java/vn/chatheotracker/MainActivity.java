package vn.chatheotracker;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.media.projection.MediaProjectionManager;
import android.media.projection.MediaProjectionConfig;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.EditText;
import android.text.InputType;

public final class MainActivity extends Activity {
    private static final int REQUEST_CAPTURE = 101;
    private TextView state;
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        LinearLayout root = new LinearLayout(this);
        root.setPadding(36, 64, 36, 20); root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(20,22,29));
        TextView title = text("CHẶT HEO TRACKER",24,Color.WHITE); root.addView(title);
        root.addView(text("v1.7 • AI offline • Gợi ý về sớm • Android 16",15,0xffd0d0d0));
        state=text("Chưa theo dõi",16,0xffa5e8a3);root.addView(state);
        Button overlay = button("1. Cho phép cửa sổ nổi", () -> {
            Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));startActivity(i);
        }); root.addView(overlay);
        Button start=button("2. Bắt đầu theo dõi",this::startCapture);root.addView(start);
        Button stop=button("Dừng theo dõi", () -> {
            Intent it = new Intent(this,CaptureService.class);
            it.setAction(CaptureService.ACTION_STOP);startService(it);state.setText("Đã yêu cầu dừng");
        });root.addView(stop);
        root.addView(button("Đặt lại khi bắt đầu ván mới", () -> {
            Intent it=new Intent(this,CaptureService.class);
            it.setAction(CaptureService.ACTION_RESET);
            startService(it);
            state.setText("Đã yêu cầu làm mới bộ bài");
        }));
        root.addView(button("Quét lại, giữ dữ liệu ván đang chơi", () -> {
            Intent it=new Intent(this,CaptureService.class).setAction(CaptureService.ACTION_RESCAN);
            startService(it);
            state.setText("Đã yêu cầu quét lại, không xóa lịch sử bài.");
        }));
        root.addView(text("Lịch sử đối thủ: từ TRÁI sang PHẢI theo thời gian. Lượt mới nhất nằm bên phải, tô vàng. Không hiển thị nhãn P1/P2/P3.\n\nChạm ↻ đặt lại bộ đếm; GIỮ ↻ để quét lại mà không xóa bài. Gợi ý tự hiện khi đến lượt, tự ẩn sau 5 giây.",14,0xffdddddd));
        root.addView(text("CHẾ ĐỘ CHƠI",17,Color.WHITE));
        Spinner gameMode=new Spinner(this);
        final String[] modes={StrategyMode.PLACEMENT.label,StrategyMode.FIRST.label};
        gameMode.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,modes));
        String savedMode=getSharedPreferences("tracker_strategy",MODE_PRIVATE)
            .getString("mode",StrategyMode.PLACEMENT.key);
        gameMode.setSelection(StrategyMode.fromKey(savedMode)==StrategyMode.FIRST?1:0);
        gameMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            @Override public void onItemSelected(AdapterView<?> parent,View view,int position,long id){
                StrategyMode chosen=position==1?StrategyMode.FIRST:StrategyMode.PLACEMENT;
                getSharedPreferences("tracker_strategy",MODE_PRIVATE).edit()
                    .putString("mode",chosen.key).apply();
            }
            @Override public void onNothingSelected(AdapterView<?> parent){}
        });
        root.addView(gameMode);
        root.addView(text("Xếp hạng 1–2–3–4: ưu tiên phương án cần ít lượt để về sớm; dùng nguy cơ bị chặn/chặt heo để cân bằng. Về nhất: thận trọng hơn với nguy cơ mất lượt. Đây là điểm chiến thuật ước lượng, không phải xác suất về từng hạng.",13,0xffbac4d2));
        root.addView(text("BÀI TAY (CHỈ SỬA TRONG APP NẾU NHẬN DIỆN SAI):",17,Color.WHITE));
        EditText hand=new EditText(this);
        hand.setTextColor(Color.WHITE);hand.setHintTextColor(0xffa9b4c6);
        hand.setHint("Ví dụ: 4♦ 6♠ 6♥ 7♣ 8♣ A♠ 2♥");
        hand.setSingleLine(false);hand.setMinLines(2);
        hand.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        hand.setText(getSharedPreferences("tracker_strategy",MODE_PRIVATE).getString("hand", ""));
        root.addView(hand);
        root.addView(button("Lưu bài tay",()->{
            StrategyAdvisor.Input handInput=StrategyAdvisor.parse(hand.getText().toString());
            if(handInput.error!=null){state.setText(handInput.error);return;}
            String handText=hand.getText().toString().trim();
            getSharedPreferences("tracker_strategy",MODE_PRIVATE).edit()
                .putString("hand",handText)
                .putString("hand_source","manual")
                .remove("table").apply();
            Intent update=new Intent(this,CaptureService.class).setAction(CaptureService.ACTION_SET_HAND);
            update.putExtra(CaptureService.EXTRA_HAND_TEXT,handText);
            startService(update);
            state.setText("Đã lưu và đồng bộ bài tay vào Tracker đang chạy.");
        }));
        root.addView(text("Khi nút ĐÁNH màu xanh xuất hiện ổn định, Tracker tự quét và gợi ý nước có mức chặn ước tính thấp nhất, kèm cảnh báo heo khi có rủi ro từ bài và lịch sử của ba đối thủ. Gợi ý tự ẩn sau 5 giây. Nếu bài chưa khớp 52 lá, Tracker sẽ quét lại và yêu cầu kiểm tra, không đoán bừa. Bạn có thể bấm Gợi ý để tính lại, hoặc sửa bài tay trong trang Tracker nếu cần. Phần trăm là mức ước tính từ mô hình hành vi, chưa phải xác suất đã được hiệu chuẩn.",13,0xffbac4d2));
        root.addView(text("TỰ HỌC OFFLINE: Tracker ghi nhớ các lượt đánh nhận diện rõ qua nhiều ván. Nếu người chơi rời phòng hoặc có người mới, lịch sử cũ được lưu trong kho tự học offline; chỉ phiên của vị trí đó được làm mới. Khi người cũ quay lại với cùng dấu hiệu tên, ứng dụng có thể nạp lại dữ liệu. Khi nút Gợi ý xuất hiện, tỷ lệ chặn ước tính được phân tách cho đối thủ Trên / Trái / Phải.",13,0xffbac4d2));
        android.content.SharedPreferences profiles=getSharedPreferences("opponent_learning_v1",MODE_PRIVATE);
        root.addView(text("Lịch sử phiên hiện tại: Trên "
            +LearnedOpponentHistory.decode(profiles.getString("moves_0","")).size()
            +" · Trái "+LearnedOpponentHistory.decode(profiles.getString("moves_1","")).size()
            +" · Phải "+LearnedOpponentHistory.decode(profiles.getString("moves_2","")).size()
            +" lượt xác nhận",13,0xffa5e8a3));
        root.addView(text("Kho lịch sử lâu dài: "+OpponentLearningArchive.decode(profiles.getString("training_archive", "")).profiles()
            +" hồ sơ đã xác nhận tên, được lưu ngay trên điện thoại. Không xóa khi người chơi rời phòng.",13,0xffa5e8a3));
        root.addView(button("Xóa dữ liệu tự học đối thủ", () -> {
            new android.app.AlertDialog.Builder(this)
                .setTitle("Xóa dữ liệu tự học?")
                .setMessage("Xóa toàn bộ kho lịch sử tự học và các phiên hiện tại. Không ảnh hưởng bài trong ván đang chơi.")
                .setNegativeButton("Hủy",(d,w)->{})
                .setPositiveButton("Xóa",(d,w)->{
                    getSharedPreferences("opponent_learning_v1",MODE_PRIVATE).edit()
                        .remove("moves_0").remove("moves_1").remove("moves_2")
                        .remove("training_archive").apply();
                    Intent it=new Intent(this,CaptureService.class).setAction(CaptureService.ACTION_CLEAR_LEARNING);
                    startService(it);
                    state.setText("Đã xóa các lịch sử học đã lưu.");
                }).show();
        }));
        root.addView(text("Quyền riêng tư: hình ảnh và lịch sử học được xử lý trên thiết bị, không truy cập Internet.\nĐể quét cả bàn, chọn 'Toàn bộ màn hình' khi cấp quyền ghi màn hình.",13,0xffbdbdbd));
        ScrollView outer=new ScrollView(this);outer.addView(root);setContentView(outer);
    }
    private TextView text(String val,int sp,int color) {
        TextView tv = new TextView(this);tv.setText(val);tv.setTextColor(color);tv.setTextSize(sp);tv.setPadding(0,0,0,25);return tv;
    }
    private Button button(String label,Runnable action) {
        Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setOnClickListener(v->action.run());return b;
    }
    private void startCapture() {
        if(!Settings.canDrawOverlays(this)) {state.setText("Cần cấp quyền hiển thị trên ứng dụng khác trước.");return;}
        MediaProjectionManager pm=(MediaProjectionManager)getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        Intent capture=(Build.VERSION.SDK_INT>=34)
            ? pm.createScreenCaptureIntent(MediaProjectionConfig.createConfigForDefaultDisplay())
            : pm.createScreenCaptureIntent();
        startActivityForResult(capture,REQUEST_CAPTURE);
    }
    @Override protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data);
        if(request!=REQUEST_CAPTURE)return;
        if(result!=RESULT_OK || data==null){state.setText("Bạn chưa cấp quyền ghi màn hình.");return;}
        Intent it = new Intent(this,CaptureService.class);
        it.setAction(CaptureService.ACTION_START);
        it.putExtra(CaptureService.EXTRA_RESULT,result);
        it.putExtra(CaptureService.EXTRA_DATA,data);
        if(Build.VERSION.SDK_INT>=26)startForegroundService(it);else startService(it);
        state.setText("Đang khởi động. Hãy quay lại game.");
    }
}
