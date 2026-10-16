# Chặt Heo Tracker v1.3 — Gợi ý nước khó bị chặn

Đây là **mã nguồn Android thử nghiệm**, chưa phải APK đã kiểm chứng trên Xiaomi. Cập nhật repository GitHub bằng toàn bộ nội dung ZIP và chạy workflow `Build Chặt Heo Tracker APK` trong GitHub Actions.

## Hành vi khi chơi

- **Chỉ khi nhấn Gợi ý:** quét lại nhiều khung hình của tay bài, quân đang trắng nổi ở giữa bàn, lịch sử còn hiện trên bàn và bộ đếm 3 đối thủ.
- **Chỉ trả một quyết định:** `ĐÁNH <bài>` hoặc `BỎ LƯỢT`. Dòng thứ hai, nếu đủ dữ liệu: `X% mẫu đối thủ có thể chặn/chặt`.
- **Nếu không khớp 52 lá:** chỉ hiện `QUÉT LẠI BÀI VÀ LỊCH SỬ`, không phỏng đoán. Nếu bài đang cầm chưa đọc rõ có thể dùng nút **Bài tay** để sửa.
- **Cảnh báo heo tự động:** khi xác nhận nút **ĐÁNH** màu xanh của game qua hai khung hình, tay bạn có quân 2 và mẫu chia bài cho thấy có khả năng bị chặt (ngưỡng 5%), hiện cảnh báo 5 giây một lần cho lượt đó. Không tự chọn hay đánh bài.
- **Lớp phủ Gợi ý tự ẩn sau 5 giây.** Các ô bài còn lại giữ nguyên bố cục cũ.

## Giới hạn phải biết

1. `X% mẫu có bài để chặn` là **tỷ lệ trên 192 cách chia bài ngẫu nhiên**, KHÔNG phải xác suất đối thủ thực sự chặn hoặc xác suất thua. Mô phỏng chưa mô hình hóa chính xác ý định và phong cách từng người chơi.
2. Bộ so sánh chỉ hỗ trợ các bộ thông thường (đơn, đôi, ba, tứ quý, sảnh, đôi thông cùng loại); quy định chặt đặc biệt, luật hàng và mức phạt tiền của phòng Chặt Heo chưa được xác minh. **Không diễn giải số % như độ chính xác đã được kiểm định.**
3. Nhận diện lượt dùng màu và vị trí nút **ĐÁNH** từ video đã gửi (khung hình chuẩn 2048×942); độ phân giải, hiệu ứng và giao diện khác có thể làm nhận diện thất bại. Khi chưa chắc chắn, app từ chối đưa lời khuyên.
4. Quét lại lịch sử chỉ khôi phục được lá **đang nhìn thấy** trong các khung hình thu được. Không thể phục hồi chắc chắn lá đã biến mất hoàn toàn khỏi màn hình trước đó nếu bị bỏ sót. Khi dữ liệu không khớp, cần hiệu chỉnh thủ công hoặc bắt đầu lại từ ván mới.
5. Nhận diện tay bài hiện dùng mẫu ảnh; một số kiểu xòe bài, lá bị chọn nhô cao, chat/icon che hoặc ảnh động có thể làm nhận diện thất bại. App không tự điền bài bị che.

## Build trên GitHub

1. Giải nén nội dung ZIP vào nhánh `main` của repository, giữ thư mục `.github/workflows`.
2. Mở **Actions → Build Chặt Heo Tracker APK → Run workflow**.
3. Chờ bước kiểm thử Java và `Build debug APK`. Tải artifact `ChatHeoTracker-v1.3-lowest-block-risk-apk` chứa `app-debug.apk`.
4. Cho phép quyền ghi màn hình và cửa sổ nổi trên Android. Nếu cài đè APK debug khác khóa ký, có thể cần gỡ bản cũ (dữ liệu cũ sẽ mất).

## Trạng thái kiểm chứng

- Kiểm thử Java về logic so quân, kiểm tra số bài, các lớp cảnh báo và thuật toán mô phỏng: đạt trong môi trường hiện tại.
- Bộ phát hiện nút ĐÁNH màu xanh đã được thử trên ảnh trích từ video: khung 30s/55s nhận diện, khung 20s/60s không nhận.
- **Chưa biên dịch Android APK và chưa thử trực tiếp trên thiết bị.** Nếu GitHub báo lỗi, cần xử lý theo log thực tế.
