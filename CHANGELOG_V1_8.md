# Chặt Heo Tracker 1.8 – quét bài + gợi ý gọn

## Sửa quét bài
- Đã xác định trên ảnh mẫu: quạt bài đủ 13 lá bắt đầu gần vị trí x=467, y=716 khi canvas 2048x942. V1.7 dùng một điểm y=715 cố định; v1.8 tìm dải y=706–723 và thử x/y lệch nhẹ. Vẫn đòi nhận diện bộ bài ổn định, không đoán lá bị che.
- Mỗi lượt quét hiện trạng thái riêng: đã nhận hình, không thấy quạt, đã đọc n lá, lệch dữ liệu hoặc mất luồng ghi hình.
- V1.8 **không** được kiểm thử trực tiếp trên Xiaomi 17 Pro Max; không thể khẳng định lỗi quét hết hẳn chỉ dựa trên ảnh tĩnh.

## HUD
- Nhãn quét cạnh nút Gợi ý ở góc trái, không chiếm vùng bài tay hoặc che lịch sử bên trái.
- Không còn dòng chẩn đoán nằm trong bảng Chưa đánh.
- Gợi ý hoàn chỉnh hiển thị trong 5 giây, bảng quét chỉ hiển thị trạng thái ngắn.
- Lớp phủ thông tin dùng chế độ không chặn chạm (FLAG_NOT_TOUCHABLE).

## Chiến thuật offline
- Bổ sung đánh giá **hình dạng bài còn lại** sau mỗi nước: các quân lẻ rời rạc, đôi/sảnh có khả năng ghép và quân kiểm soát. Đây là một điểm cộng/trừ nhỏ khi so sánh chiến thuật; không thay thế mô phỏng hành vi đối thủ.
- Vẫn chạy Monte Carlo theo lịch sử và chế độ xếp hạng 1–2–3–4; không gọi đây là AI đã huấn luyện, không coi % dự báo là xác suất được hiệu chuẩn.

## Giới hạn
- Nếu HyperOS dừng quyền ghi màn hình, phải mở lại Tracker và cấp quyền mới; bấm Reset không thể tự tái cấp quyền.
- Khi bị icon/chat che kín quân từ đầu đến cuối, không thể khôi phục giá trị quân bằng ảnh.
- Các tình huống bài bị chọn nhô cao, hiệu ứng game mới và tỷ lệ màn hình khác vẫn cần kiểm thử thực tế.
