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
import android.widget.Spinner;
import android.widget.ScrollView;
import android.widget.TextView;

public final class MainActivity extends Activity {
    private static final int REQUEST_CAPTURE = 101;
    private TextView state;
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        LinearLayout root = new LinearLayout(this);
        root.setPadding(36, 64, 36, 20); root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(20,22,29));
        TextView title = text("CHẶT HEO TRACKER",24,Color.WHITE); root.addView(title);
        root.addView(text("Bản thử nghiệm • Xiaomi / Android 16\nTự quét bài giữa bàn từ hình ảnh màn hình. Chỉ hoạt động trong game Tiến Lên – 13 – Pig Hunters với giao diện như ảnh mẫu.",15,0xffd0d0d0));
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
        root.addView(text("Bố cục v0.6: P1 (trên), P2 (trái), P3 (phải) đặt dưới mỗi đối thủ.\nBạn/khác và CHƯA ĐÁNH nằm trong dải nhỏ phía trái–giữa, ngay trên tay bạn; vùng bên phải chừa cho Hủy/Đánh.\n\nCác bảng không nhận thao tác chạm và có thể bấm xuyên qua. Các chất cùng số được gộp gọn (ví dụ 7♠♣♦♥), tự xuống dòng, KHÔNG vuốt ngang. Đỏ = cả 4 lá cùng số chưa xuất hiện, chỉ là cảnh báo khả năng tứ quý.\n\nTự động phân biệt người đánh theo số bài giảm; lá chưa xác định được người sẽ nằm trong Bạn/khác. Có thể mở Tracker và bấm Đặt lại nếu bộ đếm chưa tự đặt lại.",14,0xffdddddd));
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
