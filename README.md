# Chặt Heo Tracker v1.2 — nhận diện bài tay & Gợi ý 5 giây (bản thử nghiệm)

## Thay đổi chính

- **Chế độ chơi lưu trong app:** `Xếp hạng 1–2–3–4` (mặc định) hoặc `Ưu tiên về nhất`. Các điểm số chỉ là **heuristic**, chưa có bảng tiền/phạt chính thức và không phải xác suất thắng.
- **Tự đọc đủ 13 lá bài tay** trong quạt bài sau khi chia trên bố cục ngang 2048×942 được chuẩn hóa từ ảnh mẫu. Cần thấy đủ 13 lá, chất và số rõ ràng, cùng một kết quả ổn định qua **3 khung hình**. Không tự điền quân khi bị che, đang di chuyển hoặc chỉ còn một phần tay bài.
- **Khu vực ngay trên bài của bạn:** riêng `Tôi (n)` hiển thị những lá đang cầm, `Khác` ghi bài đã đánh mà chưa xác định được người, `Chưa đánh` hiển thị các lá chưa ghi nhận ra bàn. Không trộn bài trên tay vào bài đã đánh.
- Nếu bạn đánh một lá được nhận diện ở giữa bàn, ứng dụng **loại nó khỏi bài đang cầm**. Nhận diện sai/bỏ sót vẫn có thể khiến tay bài lệch; trước khi gợi ý, bạn nên đối chiếu dữ liệu.
- Nút **Bài tay** ngay trên HUD cho phép sửa/quét lại thủ công mà không cần thoát game. Tay bài sửa thủ công được ưu tiên cho tới khi bắt đầu ván mới.
- Nút **Gợi ý** chỉ hoạt động khi bạn bấm; khi có kết quả, popup **tự ẩn sau 5 giây**, không điều khiển hay chạm vào game.
- Tự reset khi nhận đủ 13 lá của cả ba đối thủ trong nhiều khung hình sau khi ván trước có thay đổi, có nút ↻ reset thủ công.
- Bài đối thủ được xếp lịch sử **cũ bên trái → mới bên phải**, không có nhãn P1/P2/P3.
- Giữ cảnh báo màu đỏ khả năng tứ quý và xanh khả năng 3–4 đôi thông dựa trên những lá bài **không được xác định đang ở tay mình** và chưa thấy đánh.

## Hạn chế cần biết

1. **Chỉ nhận bài tay tự động khi toàn bộ quạt 13 lá xuất hiện rõ**, đúng giao diện/mức phóng to và bố cục mẫu. Nếu bắt đầu theo dõi muộn khi trên tay còn dưới 13 lá, cần bấm **Bài tay** để sửa.
2. Quá trình nhận diện **được hiệu chỉnh trên vài ảnh và video do người dùng cung cấp**. Chưa có kiểm thử độc lập đủ lớn hoặc thử máy Xiaomi/Android 16 thực tế với phiên bản mới.
3. Bộ chiến thuật hiện là **Monte Carlo 192 cách chia bài** (chỉ khi trạng thái bộ 52 lá nhất quán), kết hợp quy tắc và điểm ước lượng. **Chưa phải mô hình AI được huấn luyện**; không khẳng định kết quả thắng hoặc đúng luật phòng đặc thù.
4. `Bài đang cần chặn` có thể cần nhập/sửa thủ công trong Tracker. Khi dữ liệu không rõ, ứng dụng không thể bảo đảm gợi ý hợp lệ.
5. Tính năng hỗ trợ trong khi chơi có thể bị giới hạn bởi quy định của game. Ứng dụng chỉ đọc nội dung hiển thị khi bạn cấp quyền, không can thiệp vào game hoặc thực hiện lượt đánh.

## Cách build

1. Tải đầy đủ thư mục gốc lên GitHub, bao gồm `.github/workflows/build-apk.yml`.
2. Vào **Actions → Build Chặt Heo Tracker APK → Run workflow**.
3. Chờ tất cả bước xanh rồi tải artifact `ChatHeoTracker-v1.2-auto-hand-ai-5s-apk`.
4. Giải nén, cài `app-debug.apk` lên thiết bị và cấp quyền cửa sổ nổi, ghi màn hình.

**Tình trạng:** Kiểm thử Java phần chiến thuật, cảnh báo, lịch sử và xác nhận nhiều khung hình đã chạy. Có đối chiếu bộ nhận diện bài tay với các khung hình mẫu. Chưa biên dịch được APK trong môi trường này vì không có Android SDK hoặc kết nối tải SDK; cần chạy workflow để xác minh biên dịch Android.
