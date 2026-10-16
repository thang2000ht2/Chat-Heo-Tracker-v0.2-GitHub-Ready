# GitHub Actions — v1.1 Monte Carlo Strategy

1. Giải nén ZIP v1.1, tải **toàn bộ nội dung** lên repository GitHub; cần giữ `.github/workflows/build-apk.yml` đúng đường dẫn và **thay thế file cũ**, không lồng một thư mục mới.
2. Vào **Actions** → **Build Chặt Heo Tracker APK** → **Run workflow**.
3. GitHub lần lượt chạy kiểm thử Java, cài Android SDK và build. Khi tất cả đều xanh, tải artifact **ChatHeoTracker-v1.1-monte-carlo-preview-apk**.
4. Giải nén artifact lấy `app-debug.apk`, cài thử trên Xiaomi 17 Pro Max. Nếu bị xung đột chữ ký, kiểm tra bạn đang cài trên app đã được ký khác; nên sao lưu dữ liệu trước khi gỡ bản cũ.

## Cách dùng Gợi ý

- Mở Tracker, nhập **toàn bộ bài còn trên tay hiện tại**, lưu lại.
- Khi cần đè một bộ trên bàn, nhập lá tương ứng vào **Bài đang cần chặn**, lưu lại; để trống khi bạn được quyền mở lượt.
- Quay về game, bấm **Gợi ý** cạnh ↻ góc trái. App chạy xử lý trên luồng nền, hiển thị tối đa 3 lựa chọn và tự ẩn sau 16 giây.
- Có đủ 3 số đếm đối thủ khớp 52 lá mới chạy 192 mẫu chia bài; thiếu dữ liệu thì chỉ phân tích bài tay và cảnh báo **chưa mô phỏng**.

**Không có mô hình AI đã train trong ZIP**. Đây là baseline tìm nước kết hợp Monte Carlo; chưa có đánh giá xác suất thắng trên ván thật. Luật chặt và tiền phạt thực tế vẫn phải xác minh.

Để train mô hình mạnh hơn, xem `AI_TRAINING_ROADMAP_VI.md`.

Lịch sử đối thủ: **lượt cũ bên trái → lượt mới bên phải**, mới nhất tô vàng, không ghi P1/P2/P3.

**Trạng thái xây dựng:** Java tests chạy được trong môi trường hiện tại; chưa biên dịch APK ở đây vì thiếu Android SDK và không có kết nối Internet để tải SDK. Quy trình GitHub cần được chạy thực tế.
