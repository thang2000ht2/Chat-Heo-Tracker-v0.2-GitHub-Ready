# Chặt Heo Tracker v3.2 — Startup Scan & Six-Card Recovery

## Vấn đề có bằng chứng trong log ngày 18/10/2026
- Có những phiên MediaProjection chỉ chụp một khung hình trước khi người dùng mở lại Tracker; một phiên kéo dài nhưng không thấy bài. Không đủ bằng chứng rằng quyền ghi hình Android bị lỗi: khi hộp cài đặt Tracker chiếm màn hình, khung hình không có game là bình thường.
- Đã có ít nhất một lần bộ đếm đối thủ giảm sáu (7→1), lớp sáng từng nhận đủ sáu quân nhưng cuối cùng ghi `incomplete_move`.
- Hệ thống thường chỉ ghi các mảnh nhỏ trước khi lớp sáng hiện đủ cả bộ; việc ghép mảnh được giữ làm đường chính nhưng có thể bỏ lỡ animation ngắn.

## Thay đổi
1. Sau khi cho phép ghi màn hình, Tracker tự thu nhỏ Activity để quay lại game (không tự ý cấp quyền).
2. Trạng thái khởi động tách khỏi lỗi đọc bài: nếu chưa thấy bất kỳ đặc trưng nào của game, thử nối lại surface **một lần** sau ít nhất 12 giây; sau 22 giây hướng dẫn người dùng mở game/kiểm tra quyền. Không xóa lịch sử.
3. Khi có một counter drop sáu lá đã được xác nhận nhiều khung, và lớp bài trắng đọc được đủ sáu quân **hợp lệ** trong vòng 4,2 giây, ghi **một** lượt nguyên tử; hỗ trợ 3 đôi thông 8–9–10 và sảnh sáu lá. Không tạo quân bị khuất, không chấp nhận bài đã ra / nằm trên tay người chơi, không gán nếu nhiều đối thủ cùng có drop sáu.
4. Log thêm `capture_waiting_game`, `capture_game_not_visible`, `capture_game_visible`, `capture_first_game_frame`, `six_card_play_recovered`.
5. Tự học chỉ lưu sự kiện sáu lá khi đầy đủ và có thể gán người dựa trên bộ đếm đã ổn định.

## Giới hạn
- Không bảo đảm mọi biến thể luật Pig Hunters đúng với bài sáu lá hoặc chặt chồng.
- Các trường hợp detector không nhìn thấy đủ sáu quân trên một khung vẫn sẽ không được khẳng định đã quét đủ; chúng được giữ làm dữ liệu thiếu và báo log.
- Chưa kiểm thử APK thực tế trên thiết bị.
