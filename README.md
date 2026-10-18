# Chặt Heo Tracker v3.1 — Active-Only Board Capture

Đây là mã nguồn Android offline để GitHub Actions build APK. **Không yêu cầu quyền INTERNET.**

Bản 3.1 sửa lỗi 2♥ được ghi nhầm là đã đánh: phân tách bài trắng lượt hiện tại khỏi bài xám lịch sử, chỉ ghép lượt khi lớp trắng ổn định, và tăng xác nhận bộ đếm 0 để tránh sự cố 2→0→2.

### Bằng chứng & kết quả kiểm thử

Ảnh do người dùng gửi ngày 18/10/2026 được dùng để làm bài kiểm tra *đã che danh tính*, `test_images/real_2h_active_jd_gray.png`. Bộ nhận diện trả về `active=[2H]`, trong khi bộ đọc cả lớp là `all=[JS,2H]`, chứng minh lớp cũ có thể làm nhiễu dữ liệu. `TestTwoHeartsRegression` tái tạo tình huống lịch sử `2H` nhưng bàn trắng `2C` và xác nhận không được ghép sai.

**38 kiểm thử Java và 5 bài kiểm tra ảnh theo nhóm đã chạy đạt tại môi trường phát triển; chưa biên dịch APK hoặc thử trên Xiaomi.**

### Cài qua GitHub

1. Giải nén ZIP, tải **nội dung** dự án lên gốc repository hiện tại.
2. Chọn **Actions → Build Chặt Heo Tracker APK → Run workflow**.
3. Tải artifact `ChatHeoTracker-v3.1-current-layer-heo-fix-apk` và cài bản APK.
4. Nếu còn lỗi, xuất `chat_heo_scan_diagnostics.jsonl` trong màn hình Tracker và gửi kèm ảnh chụp đúng thời điểm.

Dữ liệu tự học và gợi ý hiện tại được giữ nguyên. Bản này không tự khẳng định % chặt heo đã được hiệu chuẩn. Xem `CHANGELOG_V3_1.md` để biết rõ nguyên nhân và thay đổi.
