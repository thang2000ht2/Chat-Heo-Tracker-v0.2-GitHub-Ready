# Thiết kế AI offline v1.7

- Tạo các tay bài đối thủ giả lập phù hợp số quân nhìn thấy, loại trừ quân của bạn và quân đã xuất hiện.
- Mô hình hành vi cập nhật từ các lượt đánh đã xác nhận; không suy đoán rằng mọi lần bỏ lượt đều do không có bài chặn.
- Chấm mỗi nước bằng số lượt **tối thiểu ước lượng** để hoàn tất phần bài còn lại (DP trên bộ có thể đánh), cộng chi phí mất quyền, nguy cơ bị chặt và sự cấp bách khi đối thủ còn 1–4 lá. Điểm càng thấp càng được ưu tiên.
- Tỉ lệ chặn đưa ra là mức dự báo dựa trên mẫu Monte Carlo và hành vi suy đoán, **không phải xác suất thực tế đã được hiệu chuẩn**.
- Hồ sơ học của người từng ngồi ghế được lưu vào kho offline theo dấu hiệu vùng tên ổn định; giới hạn 48 danh tính và 100 mục gần nhất cho mỗi hồ sơ. Xóa thủ công sẽ xóa kho.
- Không dùng mạng nơ-ron huấn luyện; dữ liệu quan sát có thể dùng trong quy trình kiểm định/học tham số ở các phiên bản sau. Tự động lưu dữ liệu không đồng nghĩa đã tự đào tạo mô hình.
- Khi chưa xác minh được đầy đủ bộ 52 lá, từ chối gợi ý để tránh gợi ý sai.
