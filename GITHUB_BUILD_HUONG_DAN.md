# Build Chặt Heo Tracker v3.5

1. Giải nén ZIP và tải **nội dung bên trong thư mục dự án** lên repository cũ.
2. Chạy Actions → `Build Chặt Heo Tracker APK` → `Run workflow`.
3. Mã nguồn tự kiểm tra **các test Java thuần, ảnh bộ đếm thật, ảnh bài tay/bàn, công cụ phát lại và dữ liệu JSONL** trước khi biên dịch Android.
4. Artifact APK: `ChatHeoTracker-v3.5-hand-ledger-safety-apk`, file `app-debug.apk`.
5. Artifact kiểm tra phụ: `Frame-Replay-Diagnostics-v3.5` chứa CSV của bài smoke test hình ảnh (không phải đánh giá độ chính xác toàn ván).

Đây là công việc chạy trên GitHub, không phải APK đã được biên dịch trong môi trường của mình. APK phải thử trên thiết bị thật, đặc biệt ở những vị trí có số 6/8 và lượt sáu đến tám quân.
