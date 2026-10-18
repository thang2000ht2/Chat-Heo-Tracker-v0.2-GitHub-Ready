# Build APK Chặt Heo Tracker v2.8

1. Giải nén ZIP và upload **nội dung gốc của thư mục** vào repository GitHub của bạn (gồm `app/`, `.github/`, `test_pure/`, `test_android_stubs/`, `test_images/`, `training_tools/`, `evaluation/`).
2. GitHub > Actions > **Build Chặt Heo Tracker APK** > Run workflow.
3. Các bài kiểm thử Java thường và kiểm thử `HandRecognizer` trên 2 ảnh bài đã được ẩn danh sẽ chạy trước khi Gradle build Android.
4. Nếu thành công tải artifact `ChatHeoTracker-v2.8-hand-visual-repair-apk`, giải nén và cài `app-debug.apk`.

**Chú ý:** APK debug của các phiên build khác khóa có thể không cài đè được. Gỡ app cũ có nguy cơ xóa dữ liệu học offline; xuất JSONL và sao lưu trước khi gỡ. App chưa được thử trực tiếp trên Xiaomi/HyperOS trong môi trường phát triển này.
