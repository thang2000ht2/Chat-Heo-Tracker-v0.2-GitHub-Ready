# Chặt Heo Tracker v2.1 — Giữ HUD, nhịp đánh đầu ván, tự phát hiện chia ván

## 1. Gợi ý không che bài

- Bỏ đoạn mã cố tình ẩn `panels[3]` khi hiện gợi ý trong `CaptureService.showAdvice`.
- Bài trên tay, nhóm `Bạn/khác` và `Chưa đánh` vẫn hiển thị trong 5 giây gợi ý.
- Gợi ý chỉ là popup nhỏ ở mép trên, không phải bảng thay thế cho HUD bài tay.

## 2. Chiến thuật không xả heo quá sớm

- Thêm `OpeningTempoPolicy` vào lựa chọn dựa trên hành vi và cả gợi ý khi lịch sử chưa đủ.
- Khi nhiều quân còn trên tay và đối thủ chưa gần về: ưu tiên xử lý lá đơn thấp (3, 4, 5, 6, 7...), tránh dùng sớm A, K và quân 2 để giữ khả năng lấy lượt.
- Nhấn mạnh mức phạt xả nhiều heo cùng lúc ở đầu ván, nhưng **không phạt** nước có thể về ngay và giảm rất mạnh mức phạt khi đối thủ còn 1–3 quân.
- Tiêu chí vẫn xét nước hợp lệ, cấu trúc bài còn lại, xu hướng đối thủ và rủi ro bị chặn/chặt. Đây là heuristic chiến thuật, **không phải AI đã học từ nhiều ván để chứng minh tỷ lệ thắng**.

## 3. Reset ván mới

- Thêm `RedealDetector`: quan sát trực tiếp 3 ô đếm 13 lá liên tục qua **7 khung hình được quét**.
- Chỉ kích hoạt khi ván cũ đã có dấu hiệu đánh bài hoặc người chơi giảm số lá.
- Không phụ thuộc việc bộ đếm cũ còn lưu 0/1/2/3 hoặc người chơi đã thay vị trí.
- Khi reset, làm sạch duy nhất trạng thái một ván; **kho dữ liệu tự học đối thủ vẫn được giữ**.
- Nếu một bộ đếm bị che hoặc OCR sai giữa chừng, chuỗi ổn định được tính lại từ đầu để giảm reset nhầm.

## 4. Nguy cơ chặt heo

- Giữ cảnh báo cho đúng **nước được đề xuất có quân 2**, dựa trên mẫu bài còn lại, số lá và mô hình xu hướng đối thủ. Không cảnh báo vì chỉ đang cầm heo.
- Phần trăm là **mức ước lượng mô phỏng**, chưa được hiệu chuẩn bằng kết quả chơi thật hoặc xác nhận tất cả luật chặt heo riêng của game.

## 5. Kiểm thử và hạn chế

- Hai bộ kiểm thử mới: `TestOpeningTempoPolicy` (có 3 quân 2 cùng bài lẻ thấp, cả đủ/thiếu lịch sử) và `TestRedealDetector` (ổn định 13/13/13, OCR gián đoạn, tránh reset lặp).
- Kiểm thử Java thuần không thay thế kiểm thử giao diện/HUD, MediaProjection và tính đúng số 13 trên Xiaomi Android 16.
- **Chưa tạo hoặc kiểm thử được APK trong môi trường này do không có Android SDK/Gradle**. Dùng GitHub Actions để biên dịch.

## Quan trọng khi nâng cấp APK

GitHub Actions chạy trên máy chủ mới có thể dùng **debug keystore khác** so với lần build trước. Nếu Android từ chối cập nhật vì chữ ký khác, việc gỡ ứng dụng sẽ xóa dữ liệu tự học lưu cục bộ. Nên dùng **cùng một khóa ký cố định** khi nâng cấp nhiều phiên bản, và tránh gỡ ứng dụng cũ nếu chưa sao lưu dữ liệu. Bản này chưa tích hợp xuất/nhập hồ sơ tự học.
