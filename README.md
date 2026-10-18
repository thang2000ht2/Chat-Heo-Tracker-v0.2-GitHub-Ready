# Chặt Heo Tracker v3.0 – Stable Redeal & Correct Local Hand

Mã nguồn ứng dụng Android offline cho GitHub Actions biên dịch APK. Không yêu cầu quyền `INTERNET`.

Bản 3.0 sửa từ **log chẩn đoán 375 sự kiện thực** và **hai ảnh chụp ngày 18/10/2026**. Nguyên nhân chủ yếu là tự reset ván liên tục trong lúc 13/13/13, không phải do OCR không đọc được bài:

- Trước đây bộ dò reset được kích hoạt bằng các lá trên bàn hoặc lịch sử chờ, khiến ván vừa bắt đầu đã bị xóa liên tục. Bộ mới bắt buộc đối thủ có ít hơn 13 lá ổn định rồi mới xác nhận trở về 13/13/13 qua 7 khung.
- Giữ nguyên lịch sử khi chuyển ván chưa được xác nhận; chỉ reset đúng một lần cho mỗi vòng bộ đếm.
- Đối chiếu bài tay rời khỏi quạt và các lá trên bàn để ghi đúng các nước **bạn** đánh khi đối thủ còn 13.
- Khi nghi đang ở màn kết quả (đã có đối thủ 0 lá và quạt bài bạn không thấy qua 8 lần quét), ẩn tạm thông tin quân cuối khỏi HUD, không xóa ledger hay giả định hạng.
- Log thêm `redeal_evidence`, `local_move_inferred`, `hand_absent_after_winner`.

## Đối chiếu ảnh thực tế

- Ảnh ván mới: **5♥ 9♥ 10♠ J♠ J♥ K♠ K♥ A♦ 2♣** — `HandRecognizer` đọc đúng 9/9.
- Ảnh cuối: **3♣ 4♦ 5♣ 6♥ 7♦** — `CardRecognizer` đọc đủ sảnh năm lá.
- Chỉ vùng ảnh bàn/bài đã ẩn tên và người chơi được mang vào bộ test. File JSONL nguyên bản chỉ được dùng để phân tích tại lượt này, **không được đóng vào mã nguồn**.

## Cách build

Giải nén ZIP, đưa **nội dung thư mục dự án** vào gốc repository GitHub, rồi chạy `Actions → Build Chặt Heo Tracker APK → Run workflow`. Tải artifact `ChatHeoTracker-v3.0-redeal-stability-log-fix-apk`.

Chạy kiểm thử bằng workflow trước khi tải APK; chưa biên dịch/cài chạy APK trực tiếp trên Xiaomi ở môi trường hiện tại.

## Giới hạn

Nếu game đổi giao diện hoặc nhận diện bộ đếm sai kéo dài, tự reset vẫn có thể bỏ lỡ hoặc nhận sai ván. Nhận diện màn kết quả hiện là cổng an toàn để **ẩn dữ liệu lỗi thời**, chưa phải OCR hạng 1–2–3–4. Tỷ lệ giữ hàng và nước AI gợi ý chưa được hiệu chuẩn từ dữ liệu thực.

Đọc `CHANGELOG_V3_0.md`, `CACH_GUI_LOG_LOI_V2_9.md` để biết chi tiết và cách gửi log khi còn lỗi.
