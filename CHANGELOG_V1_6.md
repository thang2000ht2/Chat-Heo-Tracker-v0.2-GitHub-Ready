# Tóm tắt v1.6

- Ghi lịch sử thật của mỗi đối thủ vào `LearnedOpponentHistory` (giữ qua các ván, có ranh giới ván).
- `SeatVisualSignature` + `SeatIdentityTracker` đọc khu vực nickname và xác nhận thay đổi qua nhiều khung hình. Tên đổi / ghế trống => xóa riêng lịch sử học của người đó; giữ 2 ghế còn lại.
- Sau mỗi lượt đánh đã xác nhận, học được lối đánh lẻ/đôi/sảnh/phá bộ. Lịch sử cục bộ được sử dụng trong `ContextualHandSampler` và `OpponentBehaviorModel`.
- `MonteCarloAdvisor` trình bày rủi ro chặn riêng của từng ghế, bên cạnh rủi ro tổng hợp.
- Gợi ý tự hiện khi phát hiện đến lượt, tự ẩn 5 giây. Không tự đánh.
- Nút Xóa dữ liệu tự học trong màn hình chính.
- P1 HUD di chuyển để không che khu vực tên cần cho việc xác nhận người mới.

**Lưu ý:** Đây là heuristic thích nghi (không phải mạng nơ-ron), các tỷ lệ mô phỏng chưa được hiệu chuẩn từ ván thực; việc phát hiện người đổi ghế còn phải kiểm thử trên điện thoại.
