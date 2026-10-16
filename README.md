# Chặt Heo Tracker v1.6 – AI offline tự học theo từng người chơi

Dự án Android cho **Tiến Lên – 13 – Pig Hunters**, tiếp tục từ v1.5. Không phải APK phát hành đã được kiểm thử trực tiếp. Gợi ý sử dụng Monte Carlo 192 mẫu + mô hình hành vi có điều chỉnh theo lịch sử thực sự quan sát.

## Các điểm mới trong v1.6

1. **Học từ lượt đánh:** mỗi nhóm quân đã được nhận diện và ghép với một lần giảm số bài của đối thủ sẽ được lưu vào hồ sơ người đó trên **bộ nhớ cục bộ Android (SharedPreferences)**. Giới hạn 100 mục/ghế để không tăng kích thước không kiểm soát. Không lưu ảnh màn hình, không gửi dữ liệu lên máy chủ.
2. **Học qua nhiều ván:** các mẫu được giữ khi ba người chơi vẫn ngồi cùng ghế và game chia ván mới. Phân tách các ván để **hai quân lẻ cùng số ở hai ván khác nhau không bị suy ra nhầm là phá đôi**.
3. **Tự nhận thay người:** kiểm tra ổn định hình dạng chữ tại dải tên của 3 ghế trên/trái/phải. Khi vùng tên đổi liên tục đủ nhiều khung hình, chỉ lịch sử và hồ sơ học của **ghế đó** được làm mới; dữ liệu hai ghế còn lại được giữ. Nếu ghế trống, cũng yêu cầu số lần quan sát đủ lâu và hai ghế khác phải còn hiển thị. Không truy cập tài khoản game, không lấy người chơi từ mạng; thay người chỉ là nhận diện ảnh, **có thể không phát hiện tên gần giống, có thể nhầm khi tên bị che lâu**.
4. **Gợi ý có chiến thuật:** tiếp tục mô phỏng bài úp đúng số quân đã đếm, lấy mẫu có trọng số theo lịch sử lẻ/đôi/sảnh và mức sẵn sàng phá đôi, phá bộ để chặn; ưu tiên mức bị chặn ước tính thấp rồi cân nhắc chế độ Xếp hạng 1–2–3–4 hoặc Về nhất.
5. **Mức chặn riêng từng người:** popup tự hiện khi đến lượt (5 giây) có nước **ĐÁNH ...**, mức chặn tổng hợp và ước tính **Trên / Trái / Phải**. Những % này là **đầu ra mô hình giả lập, chưa hiệu chuẩn trên dữ liệu thực tế**, không phải xác suất đã được chứng minh.
6. **Xóa dữ liệu tự học:** nút đặt tại màn hình chính Tracker; chỉ xóa hồ sơ hành vi, không tự làm lại các quân đã ra của ván hiện tại.

## Điểm cần chú ý

- **Đếm được quân chặn không đồng nghĩa chắc chắn đối thủ sẽ chặn.** Mô hình dùng các số liệu hành vi để ước lượng quyết định đó, không thể biết bài úp thực sự.
- App hiện chưa đọc chính xác sự kiện **bỏ lượt** trong mọi tình huống; không lưu hoặc học từ những lượt bỏ qua mà chưa xác minh bằng hình ảnh.
- Nếu mất số bài, mặt bài bị che hoặc tổng bài không khớp 52, Tracker từ chối đưa lời khuyên để tránh khuyến nghị sai.
- Bằng chứng từ hình ảnh tên có thể bị nhiễu bởi thông báo hoặc hiệu ứng game. Bản v1.6 phải **thử trực tiếp trên Xiaomi 17 Pro Max** để đánh giá mức độ phát hiện người rời phòng.
- Bộ luật chặt/tứ quý/đôi thông và tiền thưởng-phạt riêng của game chưa được xác minh đầy đủ. Không nên dùng tỷ lệ đang hiển thị để đặt cược hoặc coi là cam kết thắng.
- Mặc định **không cần Internet** sau khi cài đặt. AndroidManifest không khai báo quyền `INTERNET`.

## Build APK bằng GitHub Actions

1. Giải nén ZIP, tải **nội dung thư mục gốc** (kể cả `.github/workflows/build-apk.yml`) lên repository.
2. Vào **Actions → Build Chặt Heo Tracker APK → Run workflow**.
3. Chờ các bài thử Java thành công và công việc `Build debug APK` hoàn tất. Mở mục **Artifacts**, tải `ChatHeoTracker-v1.6-learning-players-apk` và giải nén lấy `app-debug.apk`.
4. Cài trên điện thoại, cấp quyền ghi toàn màn hình và cửa sổ nổi.

Mã nguồn v1.6 đã được thử bằng `javac` cho phần thuật toán thuần Java; **chưa biên dịch ứng dụng Android trong môi trường hiện tại vì không có Android SDK**. GitHub Actions có thể phát hiện các lỗi Android/API chưa thấy ở đây.
