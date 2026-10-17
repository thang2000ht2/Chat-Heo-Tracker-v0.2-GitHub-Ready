# V2.2: Offline training data & evaluation

- Ghi nhận sự kiện ván thật theo JSONL trên thiết bị, gắn ID ván và nguồn/độ tin cậy.
- Tự thu thập lượt đối thủ, số bài thay đổi, bài tay xác nhận, kết quả quét thiếu, nước AI khuyến nghị.
- Khi reset hoặc phát bài lại, tạo ID ván mới, ghi nhận ván trước chưa biết kết quả. Giữ nguyên hồ sơ học đối thủ.
- Nút ghi hạng 1/2/3/4 cho ván vừa kết thúc, xuất JSONL qua trình quản lý tệp Android, tắt/xóa dữ liệu theo ý muốn.
- Điều chỉnh trọng số suy luận hành vi nhỏ theo lịch sử mỏng: chưa đủ nhiều lượt thì được co về giá trị trung lập.
- Công cụ Python offline huấn luyện phân phối loại nước đánh có kiểm thử theo ván; không tự triển khai model vào app nếu chưa có bằng chứng chất lượng.
- Không lưu ảnh hay nickname trong nhật ký JSONL; không cần Internet.
- Giữ thuật toán gợi ý v2.1 (heuristic + mô phỏng); không giới thiệu v2.2 là AI đã được huấn luyện.
