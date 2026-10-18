# Chặt Heo Tracker v3.1 — Fix 2♥, tách lớp bàn và lọc số đếm

## Bằng chứng từ ảnh 20:46:37 và log JSONL ngày 18/10/2026

- 20:45:48: `board_fragment.cards=[2H]` trong khi `board=[2C]`.
- 20:45:50: `move_matched(seat=0,cards=[2H],board=[2C])`; gán 2♥ cho đối thủ trong lúc bàn chỉ xác nhận 2♣. Đây là **dữ liệu mâu thuẫn**, không phải bằng chứng họ đã đánh 2♥.
- 20:46:37: ảnh cho thấy 2♥ TRẮNG trên bàn và J♦ XÁM ở phía sau. Bộ Java chạy trên ảnh đã ẩn dữ liệu người chơi trả về `active=[2H]`, `all=[JS,2H]`; chữ J của lớp xám có thể bị đọc nhầm chất.
- 20:46:38: bộ đếm phải dao động `2 -> 0 -> 2` trong khoảng 0,44 giây; không đủ chứng cứ người đó về bài.

## Thay đổi đã làm

1. `CaptureService` tách rõ **bài trắng đang đánh** và **bài xám lịch sử**: chỉ `ActiveBoardEvidence` từ lớp trắng mới được đưa vào `TemporalCardEvidence`, ghép lượt và gán người đánh. Nhận diện cả hai lớp chỉ dùng để so sánh và ghi log `board_layers_filtered`.
2. `PlayBatchMatcher` không ghép với người chơi khi lớp bài trắng chưa được xác nhận ổn định hoặc đang trống, và từ chối ghép một tổ hợp không chứa những quân trắng đang hiển thị. Đây là sửa lỗi ghép 2♥ khi bàn là 2♣.
3. `CounterStabilityGate` yêu cầu 4 khung liên tiếp cho số 0, 3 cho giảm từ 3 quân trở lên, 2 cho biến động nhỏ. Giảm khả năng hiểu sai 2→0→2.
4. Thêm hồi quy `TestTwoHeartsRegression`, `TestCounterStabilityGate`, `TestTwoHeartsBoardImage` trên ảnh bàn đã ẩn avatar/tên.

## Kiểm chứng

- **38 test thuần Java**: đạt trong môi trường chạy local.
- Bộ nhận diện ảnh thật: ảnh 20:46:37 `all=[JS,2H]`, `active=[2H]`; kiểm tra lớp trắng đạt.
- Các test ảnh tay và bàn từ v2.8/v3.0: đạt.
- Kiểm thử APK/Android/HyperOS: **chưa thực hiện** tại môi trường này; cần chạy workflow và test máy thật.

## Giới hạn

Cách lọc ưu tiên **không gán sai** thay vì đoán đủ mọi nước. Một nước đi quá nhanh hoặc bài trắng bị che toàn bộ có thể bị ghi là `incomplete_move`; cần log và video để tối ưu tiếp. Hồ sơ học lâu dài từ các bản cũ không tự bị xóa, trong đó có thể còn lượt đã phân loại sai; không coi chúng là nhãn huấn luyện chắc chắn. Công cụ chưa huấn luyện lại AI trong bản này.
