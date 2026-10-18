# Chặt Heo Tracker v3.3 — Multi-signal Move Evidence

## Mục tiêu
Không dùng quy tắc máy móc "cứ giảm sáu/tám quân thì là 3/4 đôi thông". Số lá giảm **chỉ là một bằng chứng**. Bộ xử lý phải kết hợp các nguồn quan sát độc lập và **được quyền từ chối kết luận**.

## Thực hiện
1. Thêm `PlayStructure.java`: nhận dạng cấu trúc nước đánh đã thấy đầy đủ (lẻ, đôi, bộ ba, tứ quý, sảnh, ba đôi thông, bốn đôi thông). Đây là **kiểm tra cấu trúc**, không thay cho engine luật chặt Pig Hunters.
2. Thêm `MoveEvidenceFusion.java`: lưu các lần quét **chỉ lớp bài trắng** gần thời điểm biến động bộ đếm, tổng hợp các tập bài khác nhau qua nhiều khung hình; liên kết với bộ đếm đã xác nhận và đối chiếu bài tay/lịch sử. Không tự điền chất hoặc quân thiếu.
3. Thay đường hồi phục đặc biệt sáu lá trong `CaptureService` bằng đường chung `recoverVerifiedEvidenceMove`, hỗ trợ mọi số quân cho phép và nhận diện bốn đôi thông **nếu đã thấy đủ các quân qua một hay nhiều khung hình**.
4. Nếu có nhiều đối thủ có thể là người đánh hoặc hai giả thuyết ghép khác nhau gần bằng nhau: **không gán**, không đưa vào dữ liệu train. Chặn việc ghép nhầm dữ liệu lớp trắng của hai lượt khác nhau.
5. Cho phép độ trễ ngắn giữa khi lớp bài trắng biến mất và lúc bộ đếm được xác nhận, nhưng không dùng lớp bài xám để tạo lượt mới.
6. Thêm `MoveHypothesisAnalyzer.java`: khi thiếu quân, xem xét **nhiều cấu trúc hợp lệ** phù hợp với quân đã quan sát và các quân bạn giữ/đã đánh. Ví dụ thấy ba đôi 8–9–10 với giảm tám lá thì có thể là 7–8–9–10 hoặc 8–9–10–J, nếu các hạng còn đủ quân. Đây là các **khả năng**, không phải kết luận hoặc % đã hiệu chuẩn.
7. Khi chưa đủ bằng chứng, log `move_evidence_hypotheses` ghi các khả năng chưa xác nhận. Khi đã xác nhận, log `move_evidence_fused` với quân thật, hình dạng nước đi, số quan sát và nguồn ghép.
8. Nhánh ghép lượt cũ được tăng kiểm tra cấu trúc hợp lệ trước khi chốt nước.

## Đã kiểm tra
- Test pure Java gồm `TestMoveEvidenceFusion` và `TestMoveHypothesisAnalyzer` cộng các bài hồi quy trước.
- Kiểm tra dữ liệu loại trừ quân đã đánh và quân trên tay, xử lý hai người biến động cùng thời điểm, các lớp bài không tương thích và khả năng giữ lại giả thuyết khi thiếu 1–2 quân.
- Kiểm thử ảnh tĩnh cũ vẫn giữ nguyên kết quả; không có tập ảnh 4 đôi thông thực tế do người dùng cung cấp để đánh giá khả năng thị giác đối với bộ 8 lá.

## Giới hạn trung thực
- Không tự suy luận rằng một nước giảm tám là bốn đôi thông khi chưa thấy các quân phù hợp; sảnh tám lá cũng là khả năng.
- Chưa có độ tin cậy theo từng lá được hiệu chuẩn, chưa huấn luyện mô hình nhận diện mới và chưa có luật Pig Hunters chuẩn đầy đủ.
- Khi không có bộ đếm tin cậy, không thể chốt người đánh chỉ từ dự đoán về bộ bài. Dữ liệu thiếu vẫn được lưu chẩn đoán.
- Chưa build APK Android hoặc thử trên máy Xiaomi/HyperOS trong môi trường này. Kết quả test Java không thay thế kiểm chứng APK thực tế.
