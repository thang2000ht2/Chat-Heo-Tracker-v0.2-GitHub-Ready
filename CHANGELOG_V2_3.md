# Chặt Heo Tracker v2.3 – đánh giá có đối chứng (bản nghiên cứu)

**Mục tiêu:** sửa cách biểu diễn rủi ro chặn và bổ sung nền tảng kiểm chứng thuật toán, không quảng cáo 'AI đã được train' khi chưa có số liệu.

## Thay đổi trực tiếp trong mã Android
- Loại bỏ phần trăm rủi ro chặn theo từng đối thủ từ mô hình hành vi chưa hiệu chuẩn. Khi đầu ván hầu hết nước đều bão hòa, HUD ghi 'mô phỏng bão hòa; không phân biệt được nước đánh'.
- Khi các nước gần tương đương, HUD ghi rõ điều đó, không tự nhận nước thắng chắc. Các hằng số trong `OpponentBehaviorModel`/`FinishPriority` vẫn là heuristic, chưa được thay bằng model được huấn luyện.
- Cảnh báo chặt heo không đưa phần trăm tự tạo; vẫn chỉ hiện khi chính nước đang đề xuất có quân 2, có cấu trúc đe dọa trong mô phỏng và phù hợp loại bài cần đánh.
- Thêm `RiskReporting.java` và `TestRiskReporting.java` để hồi quy lỗi '≈100%'.

## Công cụ đánh giá offline mới (Python, không tự chạy trên điện thoại)
- `evaluation/tienlen_engine.py`: bộ luật TIẾN LÊN THAM CHIẾU (chưa xác minh với Pig Hunters).
- `evaluation/selfplay.py`: so sánh bốn bot heuristic, chạy cùng hạt giống chia bài rồi xoay mỗi bot qua bốn ghế, ghi kết quả theo từng cách chia.
- `evaluation/results/*`: kết quả đã chạy 2.000 cách chia bài, 4 ghế × 4 chính sách (tổng 32.000 lượt thử theo ghế); kèm khoảng tin cậy bootstrap theo cách chia.
- `evaluation/pass_evidence.py`: thử nghiệm cập nhật trọng số từ lượt bỏ **được xác nhận**, chỉ hoạt động nếu có xác suất bỏ lượt được hiệu chuẩn trên tối thiểu 200 cơ hội; không loại cứng bài úp và chưa nối vào app.
- `evaluation/evaluate_results.py`: đánh giá tập JSONL được xuất, tách hạng được xác nhận, các lượt đánh đã nhận diện và lượt bỏ được xác nhận; **không suy hạng thắng do làm theo gợi ý** nếu chưa có nhãn xác nhận nước thật.
- `evaluation/test_rules.py`, `evaluation/test_dataset.py`: kiểm thử cơ bản.

## Chưa có (rất quan trọng)
- Engine đúng mọi luật riêng của game, quyền chặt ngoài lượt, tới trắng, tiền phạt, chặt chồng có điều kiện đặc biệt.
- Máy nhận diện 'BỎ LƯỢT' tự động có độ tin cậy cao. Không ghi lượt bỏ qua suy luận từ màn hình trống.
- ISMCTS / mô hình đối thủ đã huấn luyện bằng dữ liệu chơi thật và hiệu chuẩn phân phối xác suất.
- Nghiên cứu A/B có phân nhóm gợi ý và không gợi ý, theo dõi nước thực sự đánh.
- APK đã build hoặc đã kiểm thử trên máy thật (môi trường hiện tại không có Android SDK).

**Vì vậy đây là bản v2.3 dành cho kiểm thử / thu thập dữ liệu, chưa phải bản tối ưu AI đã được chứng minh.**
