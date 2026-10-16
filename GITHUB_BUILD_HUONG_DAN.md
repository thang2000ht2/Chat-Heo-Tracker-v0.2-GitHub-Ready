# Build v1.4 bằng GitHub Actions

1. Xóa mã nguồn cũ hoặc cập nhật đè toàn bộ các file được thay đổi của v1.4 trong repository (giữ `.github/workflows/build-apk.yml`).
2. Trong GitHub chọn **Actions → Build Chặt Heo Tracker APK → Run workflow**.
3. Kiểm tra bước **Test all pure-Java rules, warnings, recognition evidence, suggestions**: phải chạy `TestOpponentBehavior` bên cạnh bảy bài test cũ.
4. Khi job thành công, tải **ChatHeoTracker-v1.4-auto-behavior-advice-apk** từ mục Artifacts và giải nén lấy `app-debug.apk`.
5. Trên Xiaomi cấp quyền **ghi màn hình** và **cửa sổ nổi**. Ứng dụng không tự chơi.

Nếu job bị lỗi, gửi dòng lỗi đầu tiên từ bước bị đỏ, không chỉ ảnh chữ `Job failed`.
