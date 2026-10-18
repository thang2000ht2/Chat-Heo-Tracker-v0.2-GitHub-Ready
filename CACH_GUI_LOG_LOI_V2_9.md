# Cách gửi log khi Tracker nhận diện sai (v2.9)

1. Mở **Tracker v2.9**, chọn **Bắt đầu theo dõi** và cấp quyền ghi toàn bộ màn hình. Chơi bình thường.
2. Khi lỗi xuất hiện, hãy chụp màn hình hoặc quay clip ngắn, ghi lại **thời điểm** và đối thủ **Trên / Trái / Phải**.
3. Không cần reset. Mở app Tracker, kéo xuống mục **NHẬT KÝ CHẨN ĐOÁN LỖI QUÉT**, bấm **Xuất log chẩn đoán lỗi quét JSONL** và chọn nơi lưu. Gửi tập tin `chat_heo_scan_diagnostics.jsonl` cùng ảnh/clip.
4. Nếu đã lỡ reset, vẫn có thể xuất log: sự kiện `round_reset` ghi rõ mốc ván mới. Nếu muốn xóa log sau khi gửi, dùng nút **Xóa log chẩn đoán**; không tác động dữ liệu huấn luyện.

## Các dòng quan trọng

- `hand_scan`: `fan_count`, `cards`, `slots` nhận diện được trong khung quạt, trạng thái đủ/chưa đủ.
- `hand_synced`: bài tay đã được cập nhật từ nhiều khung hình.
- `hand_conflict`/`hand_deck_conflict`: lá bị ghi trùng giữa tay và lịch sử hoặc tổng dữ liệu không khả thi.
- `board_fragment`: các lá trên bàn vừa đủ bằng chứng qua nhiều khung hình.
- `opponent_count`: bộ đếm cạnh avatar giảm từ bao nhiêu xuống bao nhiêu.
- `move_matched`: đã ghép đủ số lá thành một lượt cho đúng đối thủ.
- `incomplete_move`: bộ đếm giảm nhưng chưa tìm đủ lá.
- `unattributed_play`: lá đã thấy ra bàn nhưng không gán người chắc chắn.
- `frame_exception`: loại lỗi decode của Android (không lưu thông báo hệ thống chứa dữ liệu khác).

Log chỉ lưu mã lá bài, bộ đếm, trạng thái và thời gian; **không tự lưu ảnh chụp, biệt danh hay ID tài khoản**. Tuy nhiên log vẫn mô tả các nước bài và thời điểm chơi; hãy tự kiểm tra nội dung nếu bạn cần chia sẻ hạn chế.

## Giới hạn

Kiểm thử hiện tại dựa trên các ảnh tĩnh có nhãn và chuỗi sự kiện giả lập; không phải đo độ chính xác trên video thời gian thực của mọi thiết bị. Khi ghép mảnh bài thiếu, app không phỏng đoán quân còn thiếu. Khi lịch sử mâu thuẫn, màu và tỷ lệ đối thủ được giảm độ tin cậy chứ không tự sửa tổng 52 lá theo suy đoán.
