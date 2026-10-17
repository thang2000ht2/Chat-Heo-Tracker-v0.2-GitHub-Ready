# Chặt Heo Tracker v2.4 – Đo được, không giả định

Đây là bản **research preview**, không phải AI đã được huấn luyện; vẫn cần biên dịch và chạy thử trực tiếp trên máy Xiaomi. Các thay đổi ưu tiên **đo chiến thuật Java thật** và xác định các trường hợp không đủ dữ liệu để gợi ý.

1. Chạy workflow `Build Chặt Heo Tracker APK` để kiểm thử + tạo APK; không có Android SDK trong phiên làm việc tạo ZIP này.
2. Chạy workflow `Benchmark actual Java advisor (research only)` (ở tab GitHub Actions) để đánh giá với số bộ chia 20, 100, 300 (thời gian tăng nhiều). Xem artifact `Java-Advisor-vs-Baselines-RESEARCH-ONLY`.
3. Giao diện vùng đối thủ có thêm `3ĐT ~x% · 4ĐT ~y% · TQ ~z% mẫu` **chỉ khi** bài và bộ đếm hợp lệ; tỷ lệ này nói về **các tình huống chia bài giả lập**, không nói đối thủ chắc chắn đang có hàng, không phải xác suất chặt heo.
4. Trang chính có nút ghi **đối thủ bỏ lượt do người dùng xác nhận** (Trên/Trái/Phải) nếu thấy game báo. Chưa tự suy từ việc không thấy bài.
5. Nhận diện quạt bài thử tìm thêm hàng lệch; khi chưa đủ sẽ báo số góc bài đọc được, không cố đoán. Lỗi miss quét cần bộ ảnh thực có nhãn và thử trên Android mới có thể định lượng.

Đọc `CHANGELOG_V2_4.md` và `evaluation/JAVA_ADVISOR_BENCHMARK_REPORT_VI.md`.
