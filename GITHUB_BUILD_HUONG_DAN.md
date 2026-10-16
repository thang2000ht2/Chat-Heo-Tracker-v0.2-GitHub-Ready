# GitHub Actions — Chặt Heo Tracker v1.2

1. Giải nén toàn bộ ZIP, mở repository GitHub của bạn và cập nhật **các file bên trong ZIP** vào thư mục gốc (không tạo một thư mục con nữa).
2. Kiểm tra có `.github/workflows/build-apk.yml` và `app/src/main/java/vn/chatheotracker/HandRecognizer.java`.
3. Vào **Actions → Build Chặt Heo Tracker APK → Run workflow**.
4. Workflow cài JDK 17, Android SDK 35, Gradle 8.9, chạy kiểm thử Java rồi `assembleDebug`.
5. Nếu xanh hoàn toàn, tải artifact `ChatHeoTracker-v1.2-auto-hand-ai-5s-apk` và giải nén lấy `app-debug.apk`.

**Thử nghiệm:**
- Vào phòng ván mới, bật ghi màn hình toàn màn hình; khi đủ 13 quân, kiểm tra dòng `Tôi (13)`.
- Nếu không xuất hiện, bấm `Bài tay` chọn/sửa 13 quân. Đừng dùng gợi ý với dữ liệu thiếu hoặc sai.
- Chọn chế độ `Xếp hạng 1–2–3–4` hoặc `Ưu tiên về nhất` trong ứng dụng.
- Khi bấm `Gợi ý`, bảng phân tích sẽ tự biến mất sau **5 giây kể từ khi có kết quả**.
- Giữ chức năng Reset trong góc trái và kiểm tra reset tự động khi các đối thủ trở lại 13.

Nếu GitHub báo đỏ, lấy đoạn lỗi trong bước **Build debug APK**, không chỉ gửi ảnh trang đầu Actions.

**Lưu ý:** Bản nguồn này chưa được xác minh là biên dịch thành công trên GitHub; không nên coi là APK hoàn thiện trước khi Actions chạy xanh và ứng dụng được kiểm thử trên điện thoại.
