# Build Chặt Heo Tracker v2.9 với GitHub Actions

1. Giải nén ZIP. **Upload các nội dung trong thư mục gốc** (`app/`, `.github/`, `test_pure/`, `test_android_stubs/`, `test_images/`, v.v.) vào repository GitHub cũ, ghi đè file cùng tên.
2. Vào **Actions → Build Chặt Heo Tracker APK → Run workflow**.
3. Workflow chạy thử luật/thống kê, thử ảnh thực của HandRecognizer/CardRecognizer, sau đó build `app-debug.apk`.
4. Tải artifact **`ChatHeoTracker-v2.9-verified-hand-atomic-play-diagnostics-apk`** và cài APK.
5. Khi còn lỗi: **mở Tracker → Xuất log chẩn đoán lỗi quét JSONL**. Gửi `.jsonl` cùng ảnh/video và mô tả vị trí người chơi; log không lưu ảnh/nickname.

**Lưu ý:** APK debug từ repository/runner có chữ ký khác có thể không cài đè. Nếu phải gỡ bản cũ, hãy xuất dữ liệu huấn luyện trước vì dữ liệu local có thể bị mất. Chưa build/test APK trực tiếp trên HyperOS trong môi trường này.
