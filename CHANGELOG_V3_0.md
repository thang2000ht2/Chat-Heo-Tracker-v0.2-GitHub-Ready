# v3.0 – Fix reset loop, correct local-card attribution, end-screen safety

## Kết luận từ log thật của người dùng (18/10/2026)

File log đã gửi có 375 sự kiện trong khoảng 5 phút 36 giây, trong đó có:

- 14 sự kiện `round_reset`, gồm 13 lần trong ~30 giây khi trạng thái số đếm đang 13/13/13;
- 24 lần `hand_synced`, trong đó có nhiều lần đồng bộ 9 lá rồi bị reset lại;
- 3 lần `hand_conflict`, 2 lần `hand_deck_conflict`;
- 9 `incomplete_move`, 25 `unattributed_play`;
- 12 lần rút lại nước đối thủ đã học (`learned_move_retracted`).

Trong ảnh 16:18, game cho thấy ba đối thủ 13 lá, tay thật 9 lá, Tracker chỉ hiển thị **52 lá chưa đánh**. Trong ảnh 16:20, đã vào bảng kết quả nhưng HUD còn hiển thị quân cuối J♠.

## Sửa chính

1. **RedealDetector:** không bao giờ tự reset chỉ vì có quân trên bàn. Chỉ reset khi đã nhìn thấy một bộ đếm đối thủ **dưới 13 ít nhất ba khung** rồi ba bộ đếm cùng **quay lại 13 ít nhất bảy khung**. Sau reset không thể bị reset lại nếu các số đếm vẫn đứng 13/13/13. Bỏ ngưỡng kích hoạt bằng các quân `played/waiting` sai trong giai đoạn mở đầu.
2. **Giữ yên ledger khi chờ xác nhận chia ván:** không cho dữ liệu quạt bài của ván mới viết đè lịch sử ván cũ trong bảy khung xác nhận reset.
3. **LocalPlayEvidence:** khi bài tay đã xác nhận giảm số quân, và **tất cả** những quân biến mất đồng thời được nhìn thấy ở bàn, xác nhận chúng là **bài tôi đánh**, dù đối thủ chưa giảm từ 13. Gỡ các quân này khỏi lịch sử đối thủ/khác nếu trước đó bị gán nhầm. Nếu thiếu bằng chứng, không phỏng đoán lá bài.
4. **HandEndVisibility:** khi đã quan sát một đối thủ về 0 và không thấy quạt bài bản thân **8 lần quét liên tục**, thay dòng bài cũ bằng `không thấy bài · chờ ván mới` và tạm ngưng bảng cảnh báo. **Không xóa lịch sử và không tự kết luận hạng** từ hiệu ứng màn hình.
5. **Log chẩn đoán:** thêm `redeal_evidence`, `local_move_inferred`, `hand_absent_after_winner` để lần sau xác định được sự kiện đã kích hoạt thay đổi trạng thái.
6. **Kiểm thử ảnh thật đã ẩn danh:** thêm 9 lá thật trong ảnh đầu ván (5♥ 9♥ 10♠ J♠ J♥ K♠ K♥ A♦ 2♣) và sảnh 3♣–4♦–5♣–6♥–7♦ ở ảnh cuối; không đưa ảnh có tên người chơi vào ZIP.

## Phạm vi xác nhận

- Java thuần: các bài test cũ, thêm `TestLocalPlayEvidence`, `TestHandEndVisibility` và bộ test reset chống lặp.
- `HandRecognizer`, `CardRecognizer` thật kiểm thử được trên các vùng ảnh của người dùng đã ẩn nhận dạng.
- **Chưa có APK thử trên Xiaomi hoặc bộ kiểm thử luồng video đầy đủ.** Không có kết quả mới chứng minh AI mạnh hơn; thuật toán chiến thuật và mô hình xác suất vẫn ở trạng thái thực nghiệm.

**Dữ liệu huấn luyện ở các ván v2.9 bị reset liên tục cần coi là không đáng tin.** Không dùng các `round_id` chỉ tồn tại vài giây mà không có lần thay đổi bộ đếm được xác nhận để huấn luyện.
