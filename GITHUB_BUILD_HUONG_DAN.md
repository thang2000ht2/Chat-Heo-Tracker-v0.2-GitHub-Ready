# Biên dịch Chặt Heo Tracker v1.3 qua GitHub

1. Giải nén ZIP, sao chép **toàn bộ** nội dung bên trong (kể cả thư mục `.github`) vào repository GitHub.
2. Mở `.github/workflows/build-apk.yml` và kiểm tra workflow hiện phiên bản `v1.3-lowest-block-risk` / artifact mới.
3. Vào **Actions → Build Chặt Heo Tracker APK → Run workflow**.
4. Khi job màu xanh, mở **Artifacts**, tải `ChatHeoTracker-v1.3-lowest-block-risk-apk`.
5. Giải nén artifact để lấy `app-debug.apk` và cài thử trên điện thoại Android 16.

Nếu Actions báo lỗi, chụp phần chữ đỏ và đoạn trước đó trong log. Không nên đổi hàng loạt phiên bản Gradle/SDK khi chưa rõ lỗi.

**Giới hạn kiểm thử:** Các thuật toán Java độc lập đã qua kiểm thử tại đây. Chưa có Android SDK để biên dịch hoặc kiểm chứng APK thật; GitHub Actions mới là phép thử biên dịch Android. Nhận diện là cách khớp hình ảnh trên giao diện game mẫu, nên vẫn cần thử trong ván thật.
