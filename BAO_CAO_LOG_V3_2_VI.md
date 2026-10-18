# Phân tích log quét Pig Hunters — lần gửi ngày 18/10/2026

## Dữ liệu
1178 sự kiện JSONL đã đọc; 8 phiên `capture_started`, 335 lần `hand_scan`, 223 mảnh `board_fragment`, 117 lượt được ghép `move_matched`, 41 lượt `incomplete_move`, 63 lượt `unattributed_play`.

## Lỗi mở lần đầu
Ba phiên bắt đầu lúc 09:15:02, 13:28:18, 14:14:26 (UTC, cần cộng 7 giờ để ra giờ Việt Nam) đều kéo dài khoảng 0,2 giây, mỗi phiên chỉ ghi một khung `hand_scan` không tìm thấy bài; ngay sau đó người dùng mở phiên mới. Một phiên lúc 14:14:29 UTC kéo dài ~47 giây, có 9 sự kiện `hand_scan` và không có tay bài hoàn chỉnh.

Điều này cho thấy **không thấy game trong hình được chụp**, nhưng log không đủ để kết luận rằng driver MediaProjection hay bộ đọc bài bị lỗi. Android có thể đang ghi cửa sổ cài đặt Tracker thay vì game. Sửa v3.2: tự thu nhỏ Activity ngay sau khi cấp quyền, ghi log tình trạng không thấy game, thử nối lại một lần, nhắc mở game nếu tiếp tục không thấy dấu hiệu bàn chơi.

## Lỗi sáu lá
Ngày 18/10 lúc **13:41:12 UTC (20:41:12 Việt Nam)**, bộ đếm người chơi vị trí 0 giảm từ 7 còn 1 (**6 lá**). Lúc 13:41:14, log `board_fragment` cho thấy `board` gồm chính xác sáu lá `10♣ 10♠ J♣ J♠ Q♥ Q♠`. Tuy nhiên, đến 13:41:19 bộ đối chiếu ghi `incomplete_move` cho lượt giảm sáu đó.

Đây là bằng chứng kỹ thuật trực tiếp rằng bộ nhận diện **đã có lúc thấy đủ sáu lá**, nhưng bộ ghép theo mảnh và thời điểm không chốt được lượt sáu lá. Log cụ thể này là **3 đôi thông 10–J–Q**, không phải bằng chứng trực tiếp về 8–9–10 mà người dùng đã mô tả; không được đánh đồng hai trường hợp.

Sửa v3.2: đường chốt nguyên tử dự phòng, chỉ khi *cùng lúc* có một lần giảm sáu lá đã xác nhận và ảnh lớp trắng hiện tại cung cấp đủ sáu quân tạo sảnh sáu hoặc ba đôi thông. Nếu thiếu một quân, đã có quân trong lịch sử, trùng bài tay hoặc nhiều đối thủ đồng thời giảm sáu, giữ `incomplete_move` để xem lại và không học thành sự thật.

## Chưa có bằng chứng
Ảnh chụp cuối ván lúc 21:16:57 không cho thấy trực tiếp bộ ba đôi thông 8–9–10. Cần ảnh/video hoặc log ngay lúc bộ này ra bàn để kiểm tra riêng vấn đề nhận diện hình học nếu v3.2 vẫn bỏ sót.

## Kiểm thử
- 40 chương trình Java thuần đạt, gồm kiểm tra phục hồi ba đôi thông 8–9–10, từ chối 5/6 lá và chống ghép sai người.
- Các kiểm thử đọc ảnh tĩnh từ những ảnh cũ đạt, nhưng **không có ảnh gắn nhãn của 3 đôi thông 8–9–10 thực tế**.
- Chưa có Android SDK trong môi trường này nên **chưa tạo APK hoặc chạy trên điện thoại**.
