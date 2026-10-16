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
        root.addView(text("v1.3 • Gợi ý nước khó bị chặn nhất • Xiaomi / Android 16",15,0xffd0d0d0));
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
        root.addView(text("Lịch sử đối thủ: từ TRÁI sang PHẢI theo thời gian. Lượt mới nhất nằm bên phải, tô vàng. Không hiển thị nhãn P1/P2/P3.\n\nNút ↻ đặt lại bộ đếm. Nút Gợi ý chỉ phân tích khi bấm, không tự chọn hoặc đánh bài.",14,0xffdddddd));
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
        root.addView(text("Xếp hạng 1–2–3–4: coi trọng vị trí về và tránh giữ nhiều bài khi đối thủ sắp hết. Về nhất: ưu tiên con đường về đầu tiên. Đây là điểm số ước lượng, chưa mô phỏng đầy đủ tiền thưởng/phạt hoặc xác suất hạng.",13,0xffbac4d2));
        root.addView(text("BÀI TAY HIỆN TẠI (cũng có thể bấm Bài tay ngay trong game):",17,Color.WHITE));
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
            getSharedPreferences("tracker_strategy",MODE_PRIVATE).edit()
                .putString("hand",hand.getText().toString().trim())
                .putString("hand_source","manual")
                .remove("table").apply();
            state.setText("Đã lưu. Vào game, bấm Gợi ý ở góc trái khi cần.");
        }));
        root.addView(text("Trong game: bấm Gợi ý để quét lại bài tay, bài mới trên bàn và bộ đếm. App chỉ hiện 1 nước đánh kèm tỷ lệ mẫu có thể chặn, rồi ẩn sau 5 giây. Khi nút ĐÁNH màu xanh xuất hiện và bài bạn có quân 2 với nguy cơ bị chặt, app cảnh báo một lần. Nếu dữ liệu không khớp, app yêu cầu quét lại thay vì khuyên bừa. Nút Bài tay dùng để sửa nếu tay bài bị che. Đây là tỷ lệ trong mô phỏng, không phải xác suất đối thủ sẽ thực sự chặn; luật chặt đặc biệt chưa được kiểm chứng toàn bộ.",13,0xffbac4d2));
        root.addView(text("Quyền riêng tư: xử lý hình ảnh trên thiết bị, không truy cập Internet.\nĐể quét cả bàn, chọn 'Toàn bộ màn hình' khi cấp quyền ghi màn hình.",13,0xffbdbdbd));
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
