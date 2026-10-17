**v2.0:** Bản sửa điều kiện cảnh báo heo theo *nước thực tế được đề xuất*, thu gọn nhãn Quét, và gợi ý chiến thuật tạm nếu lịch sử thiếu. Xem CHANGELOG_V2_0.md.

# GitHub Actions — v1.7

1. Giải nén ZIP vào máy, chọn mọi file và thư mục **bên trong**, bao gồm thư mục ẩn `.github`.
2. Tải lên repository GitHub (tốt nhất để Private).
3. Chọn **Actions → Build Chặt Heo Tracker APK → Run workflow**.
4. Chờ kiểm thử pure-Java và `Build debug APK` thành công; tải Artifact `ChatHeoTracker-v1.7-stable-hud-offline-apk`.
5. Giải nén artifact và cài `app-debug.apk` lên điện thoại.

Lưu ý: ứng dụng chưa được biên dịch bằng Android SDK và thử trực tiếp trong môi trường tạo gói này. Nếu Actions báo đỏ, hãy gửi ảnh và đoạn lỗi gần dòng đầu tiên trong mục `Build debug APK`. Tránh đăng log có thông tin nhạy cảm.


## Bản 1.8
- Gói phát hành: `ChatHeoTracker-v1.8-scan-fix-compact-ai-apk`
- Đã thêm TestHandFanGeometry / TestResidualHandQuality, xuất APK sau khi test thuần Java qua.
- Đây vẫn là dự án nguồn chưa thử build Android trực tiếp trên Xiaomi.


### v1.9 build notes
The workflow runs `TestPartialHistoryRecovery` plus the prior pure-Java tests. If build fails after the test stage, copy the first Java/Gradle error and share it. The debug artifact is `ChatHeoTracker-v1.9-recovery-ai-warning-apk`.
