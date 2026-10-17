# Cách thu dữ liệu hành vi dụ heo — v2.7

## Có tự lưu dữ liệu khi chơi không?
Có **nếu** ở màn hình Tracker đã bật **Lưu dữ liệu chơi để huấn luyện offline (không tự train)**. Mặc định bật. Khi đang được cấp quyền đọc màn hình, ứng dụng lưu những sự kiện nhận diện xác nhận vào bộ nhớ riêng trên điện thoại (`ch_context_training/observations*.jsonl`). Không lưu ảnh, tên tài khoản, số điện thoại hoặc vị trí. Nhật ký xoay vòng khoảng 8 MB nên phải xuất định kỳ nếu chơi nhiều.

## Phân biệt dữ liệu thật và giả thuyết
- `opponent_move` + `confidence=matched_exact`: quân đã thấy và khớp mức giảm của bộ đếm đối thủ.
- `opponent_pass` + `confidence=user_confirmed`: do bạn xác nhận; có `lead_is_two` nếu bàn là một quân 2.
- `local_play_observed`: quân nằm trên tay đã xác nhận trước đó và hiện ra giữa bàn; chưa thể chứng minh chắc thao tác bấm Đánh.
- `bait_candidate`: đối thủ vừa đánh cao khi bạn còn khả năng ra heo tương ứng. **Không** phải nhãn “đối thủ thực sự nhử”.
- `advice_proposed`: nước app khuyên, **không phải** nước bạn đã đánh.
- `round_result`: kết quả hạng 1–4 do bạn nhập trong Tracker sau ván.
- `threat_sample`: kết quả mô phỏng, tuyệt đối không sử dụng làm nhãn sự thật để train model.

## Muốn AI học được chiêu mồi thì cần gì?
Cần các mẫu được xác nhận theo chuỗi: **(1) tình huống trước khi đối thủ đánh cao; (2) quân họ đánh; (3) bạn thực sự ra 2 hay không; (4) đối thủ nào chặt bằng bộ gì; (5) kết quả và tiền thưởng/phạt**. V2.7 mới tự thu phần quan sát được, chưa tự xác minh chính xác chuỗi chặt/mức phạt. Đừng huấn luyện mô hình “có hàng” bằng nhãn `bait_candidate` vì sẽ tạo ra sai lệch có hệ thống.

## Cách kiểm tra dữ liệu
1. Trong ứng dụng, bật **Lưu dữ liệu chơi để huấn luyện offline**.
2. Chơi vài ván, ghi nhận hạng nếu có, và nhấn **Xuất dữ liệu huấn luyện JSONL**.
3. Trên PC chạy `python3 training_tools/inspect_bait_signals.py chatheo-observations.jsonl --min-rounds 30`.
4. Báo cáo sẽ cho biết số ván, số lượt đối thủ xác nhận, số lần ghi nhận tín hiệu đánh cao, số lượt bỏ xác nhận và số lần bạn ra 2 được quan sát. **Nó không tạo ra xác suất có tứ quý hoặc AI đã train.**

## Khi app không gợi ý sau vòng bỏ lượt
App v2.7 có thêm kích hoạt khi bàn trống ổn định, và khi ba lượt bỏ trên chính lượt bài của bạn được xác nhận. Nếu vẫn lỗi, hãy gửi video có cả thanh nút ĐÁNH và mặt bàn **trước khi đối thủ bỏ, trong lúc bỏ và ngay sau khi đến lượt lại**. Nếu lần trước quét chưa đủ bài và sau đó quét tốt hơn, app sẽ tự thử gợi ý lại; nếu dữ liệu y hệt lỗi, app không tính lặp vô hạn.
