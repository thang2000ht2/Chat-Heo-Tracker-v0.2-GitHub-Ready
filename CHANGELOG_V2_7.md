# v2.7 — Gợi ý lại khi trở vòng · Nhật ký dấu hiệu mồi heo

## Sửa lỗi gợi ý không chạy sau khi các đối thủ bỏ lượt

- Giữ cách nhận diện nút ĐÁNH xanh nhưng thêm `AdviceReentryDetector`: khi mặt bàn từng có lượt đánh, sau đó trống ổn định **4 lần đọc** và nút ĐÁNH đang hoạt động, app đặt một yêu cầu gợi ý mới. Không suy đoán ai vừa bỏ lượt từ một ảnh trống.
- Nếu chính lượt của người chơi đã được quan sát và **ba lượt bỏ được người dùng xác nhận** ở cùng bàn, app có thể đưa gợi ý **mở lượt** ngay cả khi màn hình còn giữ hình quân đã đánh. Đây là đường dự phòng cần xác nhận thủ công; không tự gọi một hành động không nhìn thấy là bỏ lượt.
- `AdviceRetryGate`: nếu trước đó hiển thị QUÉT LẠI do dữ liệu chưa đủ, khi bài tay/bàn/lịch sử/số đếm thực sự thay đổi, app tự chạy gợi ý lại. Không tính lặp trên dữ liệu y hệt, có giới hạn thời gian giữa các lần thử.
- Tách `threatExecutor` khỏi `adviceExecutor`: tính mẫu tứ quý/đôi thông không được xếp hàng trước gợi ý đánh. Giới hạn một tác vụ tính threat cùng lúc để tránh hàng đợi đầy.

## Dấu hiệu hành vi nghi dụ heo

- Thêm `BaitBehaviorSignals` chỉ ghi nhận **hành vi thấy được**: đánh Q/K/A lẻ khi bạn vẫn giữ một con 2; hoặc đôi K/A khi bạn còn đôi 2. Bắt buộc lượt đối thủ nhận diện đủ số quân tương ứng bộ đếm.
- Sau từ **hai tín hiệu** trong cùng ván sẽ hiện ghi chú nhỏ `⚑ áp lực bài cao · chưa rõ có mồi heo` dưới avatar, không phán chắc đối thủ có hàng.
- Lưu sự kiện `bait_candidate` cùng thời điểm, ghế, quân đã đánh và kiểu tín hiệu, đánh dấu `weak_observable_pattern_not_hidden_intent`. Không đưa sự kiện này thành nhãn chắc chắn hoặc phần trăm chặt heo.
- Ghi thêm `opponent_move` (bối cảnh bài bàn gần nhất, số lá còn, bạn còn heo hay không), `opponent_pass` chống quân 2 do người dùng xác nhận, và `local_play_observed` để sau này kiểm tra chuỗi mồi → ra 2 → chặt thực sự.
- Dữ liệu **được lưu offline khi bật tùy chọn trong màn hình Tracker**, giới hạn khoảng 8 MB xoay vòng; **không tự huấn luyện mô hình ngay trên điện thoại**. Công cụ `training_tools/inspect_bait_signals.py` thống kê tín hiệu, không tự khẳng định hành vi mồi là đúng.
- Giảm ghi bản dự đoán `threat_sample` lặp lại cho cùng một trạng thái để bảo toàn chỗ cho lượt đánh thật.

## Những thay đổi được giữ từ v2.6

- Trừ đúng bài tay của bạn và bài đã thấy khỏi tập lá có thể do đối thủ giữ.
- Tô màu từng **chất** trong “Chưa đánh”: lá trong tay bạn màu trung tính, không bị tô đỏ/xanh như hàng có thể do đối thủ giữ.
- Tính lại % 3ĐT/4ĐT/TQ khi bài tay/lịch sử/số bài thay đổi (chỉ là % mẫu mô phỏng, chưa hiệu chuẩn).

## Kiểm thử

- Kiểm thử Java thuần: `TestAdviceReentryDetector`, `TestAdviceRetryGate`, `TestBaitBehaviorSignals` + toàn bộ bộ test v2.6.
- Python: `test_inspect_bait_signals.py`, bộ huấn luyện/dataset tham chiếu cũ.
- Chưa compile APK trong môi trường hiện tại; chưa thử trực tiếp trên Xiaomi. Các quy tắc chặt đặc thù của Pig Hunters chưa được xác nhận.

## Giới hạn quan trọng

Không có mô hình nào xác định được ý định giữ hàng chỉ từ hai nước bài. Màu và tỷ lệ cấu trúc dưới avatar là mô phỏng bài úp có thể có, không phải dự đoán ý đồ đã huấn luyện. Đặc biệt, nếu bàn không dọn hình, nút ĐÁNH không tạo cạnh kích hoạt, và không có xác nhận bỏ lượt, vẫn có tình huống app không nhận biết quyền đánh quay lại. Cần video cho đúng tình huống đó để sửa bộ nhận diện giao diện, không nên tự tạo sự kiện bỏ lượt.
