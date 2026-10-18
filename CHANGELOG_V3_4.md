# Chặt Heo Tracker v3.4 — Counter Evidence & Scan Diagnostics

## Đã làm

1. **Chữ số 6/8 từ ảnh thật:** thay `count_digit_6.png`, `count_digit_8.png` bằng mẫu cắt từ ảnh trò chơi do người dùng cung cấp, không sử dụng mẫu tự vẽ.
2. **Chấp nhận / từ chối dựa trên hai ứng viên:** `DigitTemplateScorer` trả điểm cao nhất, điểm thứ hai, độ chênh; không nhận số nếu điểm tuyệt đối hoặc độ chênh không đạt ngưỡng. Đó là mức tương đồng hình, không phải xác suất.
3. **Huy hiệu biến mất -> KHÔNG RÕ**, không còn mặc nhiên suy ra đối thủ còn 0 lá. Tránh gán người xem/người thắng nhầm là sự kiện đánh hết bài.
4. **Ràng buộc biến động bộ đếm:** không chấp nhận số tăng riêng lẻ trong ván, không chấp nhận >12 quân ra trong một lượt. Chỉ `RedealDetector` có quyền xác nhận chia ván mới khi cùng lúc có ba bộ đếm 13 ổn định theo quy tắc riêng. Cú giảm từ 6 lá cần ít nhất bốn khung hình khớp.
5. **Chỉ một đường ghi lịch sử đối thủ:** `CaptureService` không gọi `PlayBatchMatcher.exact` nữa. `MoveEvidenceFusion` là bộ chốt nước đánh duy nhất, xét bài trắng, lịch sử, bài tay, biến động bộ đếm và luật hình dạng. Không bịa lá chưa thấy. Các lớp ghép cũ được đánh dấu legacy.
6. **Nhật ký `counter_ocr` / `counter_rejected`:** lưu ứng viên, điểm cao nhất, điểm nhì, chênh lệch và lý do mơ hồ hoặc tăng số trái quy tắc, tại chu kỳ khoảng 8 khung hình và khi trạng thái thay đổi. Không lưu ảnh hoặc tên người chơi.
7. **Lượt chưa ghép:** khoảng 6,5 giây sau khi bộ đếm giảm mà chưa đủ quân, ghi `move_pending` nhưng vẫn tiếp tục xét bằng chứng đến 18 giây. Hết hạn mới ghi `incomplete_move` (không dùng làm nhãn nước đánh đúng).
8. **Tự kiểm tra trong MainActivity:** vào mục `TỰ KIỂM TRA NHẬN DIỆN`, nhấn `Làm mới bảng tự kiểm tra` để xem số đọc từng ghế và điểm từng quân bài tay. Điểm lá tay là điểm số/chất thấp nhất; không phải xác suất đã kiểm chứng.
9. **Kiểm thử:** 18 huy hiệu đã gắn nhãn từ ảnh thực, 17 đọc đúng, 1 từ chối, 0 nhầm; kiểm thử hình dạng nước 3–12 quân (giả lập tiến trình khung hình), phát lại các ảnh được che thông tin cá nhân, và công cụ thống kê JSONL theo ghế/số quân.
10. **CI:** test thuần Java, test ảnh, kiểm tra phát lại hình, test công cụ tổng hợp log và Gradle assembleDebug. Bản này vẫn cần bạn chạy GitHub để có APK thật.

## Chưa làm / chưa được xác nhận

- **Chưa đạt được chứng cứ 99% chính xác**: 18 mẫu ít và có ảnh gần trùng nhau; cần ảnh nhiều ván cho từng vị trí/giá trị, với nhãn độc lập và bộ test cố định.
- Mốc **0 lỗi gán sai** mới chỉ đạt trên các huy hiệu đã gắn nhãn và bài mô phỏng, không phải kiểm chứng trên toàn bộ video thật.
- Chưa có video nhiều ván với nhãn từng thời điểm để đo chính xác **frame -> ledger**, đặc biệt nước dài 6–12 quân. Runner đã có để phát lại chuỗi ảnh nhưng chưa bao phủ mã cửa sổ nổi / MediaProjection của Android.
- Không dùng lịch sử thiếu làm ràng buộc 52 lá tuyệt đối: bàn ba người, người xem và những quân đã đánh bị quét sót cần được phân biệt trước.
- Chưa sửa thuật toán chiến thuật, không tự công bố tỷ lệ thắng hay phần trăm chặt heo chưa hiệu chuẩn.
- Mô-đun lượt chơi vẫn cần thêm việc tách khỏi CaptureService; đừng coi đây là refactor hoàn chỉnh.

## Thử lại trên Xiaomi

1. Cài APK được GitHub Actions build từ **cùng mã v3.4**.
2. Mở Tracker, vào game, thử các số **6/8/3/9/5** ở ba ghế. Khi nhận mơ hồ, kiểm tra xem app giữ `?` thay vì ghi một con số sai.
3. Trong Tracker, chọn `Làm mới bảng tự kiểm tra`; chụp điểm ứng viên và bấm `Xuất log chẩn đoán lỗi quét JSONL` khi có vấn đề.
4. Gửi video/ảnh có nhãn thủ công, kể cả các trường hợp **đọc không được**, để tạo tập kiểm định độc lập.
