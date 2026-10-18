# Báo cáo nâng cấp Chặt Heo Tracker v3.4 — Có kiểm chứng, không phỏng đoán

**Mục tiêu phiên bản:** giảm xác nhận sai bộ đếm đối thủ và ghép sai lượt đánh; không chỉnh AI chiến thuật lúc hệ thống quan sát còn thiếu ổn định.

## 1. Kiểm tra bộ đếm trên ảnh đã gắn nhãn

Lấy 18 huy hiệu số của trò chơi từ ảnh người dùng cung cấp ở các vị trí Trên, Trái và Phải. Dùng lớp `CountRecognizer` thật trên JVM với Android Bitmap/Asset stubs, không viết lại logic nhận diện.

| Vị trí | Số mẫu | Đọc đúng | Đọc nhầm thành số khác | Không đọc vì mơ hồ |
|---|---:|---:|---:|---:|
| Trên | 5 | 4 | 0 | 1 |
| Trái | 6 | 6 | 0 | 0 |
| Phải | 7 | 7 | 0 | 0 |
| **Tổng** | **18** | **17** | **0** | **1** |

Trường hợp `ANH 3.jpg` tại vị trí Trên có chữ số 8 nhưng hai ứng viên sát điểm nhau: app **không trả về một chữ số nào**. Bộ test cố ý coi đó là trường hợp từ chối, không phải phép đo lỗi tự động.

**Không dùng 17/18 làm công bố độ chính xác:** phần lớn ảnh không độc lập, mẫu số 6/8 được trích từ cùng nhóm ảnh đang dùng để kiểm thử và chưa có tập đánh giá độc lập. Mục tiêu >99% cần được đo trên hàng trăm/hàng nghìn nhãn từ nhiều ván, từ nhiều mức che / thiết bị / vị trí.

## 2. Đọc điểm và kiểm tra bộ đếm

- Mẫu số 6 và 8 đã được thay bằng mẫu chữ từ ảnh trò chơi.
- Bộ nhận diện sử dụng điểm khớp cao nhất **và khoảng cách đến ứng viên thứ hai**; dưới ngưỡng trả `?`.
- Khi huy hiệu bị mất, trả `?`, không tự ghi số 0 hay chiến thắng.
- Số tăng trong ván (6→8, 2→3...) bị từ chối; chia ván mới được xác minh qua cơ chế khác.
- Kết quả `counter_ocr` trong log có `ocr_best`, `ocr_second`, `ocr_margin`, `ocr_reasons`, `ocr_runner_digits`. Sự kiện `counter_rejected` lưu trước/sau và lý do.

## 3. Ghép nước nhiều lá

Trong dịch vụ thật, đường ghi nhận lịch sử đối thủ duy nhất là `MoveEvidenceFusion`, không chạy thêm `PlayBatchMatcher` để tạo kết quả khác. Bộ hợp nhất yêu cầu bằng chứng từ quân trắng đang đánh, khung hình gần nhau, biến động bộ đếm, bài tay đã xác nhận, lá đã ghi là ra bàn và cấu trúc nước hợp lệ. Không đủ quân hay có đối thủ cạnh tranh thì không ghép bừa.

Dữ liệu chưa đủ được ghi `move_pending` và các giả thuyết, trước khi ghi `incomplete_move` nếu quá hạn. Nó không biến các quân chưa nhìn thấy thành dữ liệu huấn luyện chắc chắn.

Test tổng hợp cho phép ghép sảnh dài **3–12 lá**, giả lập các quân xuất hiện dần, và xác minh rằng thiếu một lá thì không sinh ra lá đó. **Đây không phải tỷ lệ đúng theo video thực tế.**

## 4. Replay và thống kê

- `test_android_stubs/ReplayScannerFrames.java`: chạy lại chuỗi ảnh PNG có timestamp qua các bộ đọc số, tay bài, bàn và bộ hợp nhất Java; xuất CSV theo khung hình.
- `evaluation/scan_metrics.py`: thống kê `counter_ocr`, số lượt xác nhận, đang chờ, chưa đủ theo ghế và số lá. Chỉ đo đúng/sai thật nếu có **nhãn độc lập** từ video.
- GitHub CI chạy các bài thuần Java, mẫu ảnh bộ đếm, bài tay/bàn, replay smoke, Python metrics và biên dịch APK; artifact kèm báo cáo test, không chỉ APK.

## 5. Chưa nghiệm thu các điều kiện nâng cao

- Chưa đủ ảnh độc lập để kết luận ≥99% số nhận đúng hay 0 ghi sai ngoài đời.
- Chưa có video nhiều ván gắn nhãn 1–12 lá đầy đủ để đo toàn chuỗi frame → ledger. Replay khung hình còn phải mô phỏng một số đặc tính service Android.
- Chưa đối chiếu tuyệt đối tổng 52 trong mọi trường hợp: người xem và bàn ba người có thể tồn tại 13 lá không được chia, và lịch sử quét có thể thiếu.
- Chưa chạy benchmark 1.000 ván Java ở phiên bản này; luật Pig Hunters và dữ liệu gợi ý cũng chưa đủ để coi kết quả là tỷ lệ thắng người thật.
- Chưa có APK build/kiểm thử trên Xiaomi trong môi trường này. File mã nguồn có workflow để bạn chạy CI Android trên GitHub.

**Khuyến nghị:** cài bản test, thử các bộ đếm 6/8 và nước đánh 6–8 lá; xuất JSONL và ảnh gốc khi có hai ứng viên gần điểm nhau. Giữ cố định bộ test độc lập trước khi phát triển AI mới.
