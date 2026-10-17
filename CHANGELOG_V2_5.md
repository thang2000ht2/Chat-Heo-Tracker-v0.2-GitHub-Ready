# Chặt Heo Tracker v2.5 — Threat posterior preview

- Bảng cảnh báo 3ĐT / 4ĐT / TQ hiện thường trực dưới mỗi đối thủ. Chưa nhận được số bài: dùng dấu `—` (không bịa xác suất).
- Tăng cỡ lấy mẫu từ 512 lên 1024 và 1536 khi cuối ván (ít nhất một đối thủ có tối đa 5 lá).
- Cho phép dữ liệu thiếu vẫn ước lượng ở mức yếu, không xóa các lá đã thấy và không tự nhận các lá thiếu thuộc ai.
- Nhận lượt bỏ do người chơi **xác nhận**, sử dụng như bằng chứng mềm, không loại cứng bất kỳ quân bài úp nào.
- Tính lại khi bộ đếm, lịch sử nhận diện, đối thủ thay đổi, hoặc người dùng xác nhận bỏ lượt. Kết quả tính ở background thread.
- Ghi sự kiện `threat_sample` tách biệt khỏi nhãn đã đánh để tích lũy dữ liệu phục vụ kiểm định về sau.
- Bổ sung các kiểm thử Java độc lập cho: lượt bỏ, số bài ít, bảo toàn quân bài, dấu hiệu 3/4 đôi thông và tứ quý.

**Không thay đổi bộ gợi ý chính. Không được coi đây là mô hình đã được huấn luyện hoặc số % đã hiệu chuẩn.**
