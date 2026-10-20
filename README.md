# Chặt Heo Tracker v3.5 — Chống ghi sai bài tay và lịch sử

Mã nguồn Android offline để build APK trên GitHub Actions. Không dùng quyền INTERNET.

**Ưu tiên v3.5: sửa lỗi bài tay/lịch sử từ log thật, giữ nguyên thuật toán AI.**

- Bài tay đã xác nhận **không được giảm** chỉ vì ba khung hình trông giống một quạt 1 lá. Các lá biến mất phải được nhìn thấy ở giữa bàn; nếu thiếu thì giữ trạng thái `bài cũ · chờ khớp`.
- Nếu lá trên bàn mâu thuẫn với tay bài đã lưu, HUD và gợi ý chuyển sang trạng thái chưa chắc chắn; không tự coi đây là một nước bạn đã đánh.
- Lượt không gán được người đánh vẫn được ghi là quan sát công khai, **không tự tạo nhãn chơi của bạn**.
- Danh sách `Chưa đánh` loại quân đã quan sát trên bàn, kể cả khi tay cũ còn giữ nhầm quân đó.
- Log mới: `hand_transition_deferred`, `hand_transition_recovered`, `public_hand_conflict_pending`.


- Mẫu số **6/8** lấy từ ảnh thật; từ chối nếu điểm hai ứng viên quá gần, thay vì đoán sai.
- Bộ đếm **không được tăng trong ván**; số 13 trở lại do hệ thống xác minh chia ván mới, không tự tin vào một khung hình. Huy hiệu không thấy được được ghi là `?`, không tự tính `0`.
- `MoveEvidenceFusion` là **đường ghi lịch sử đối thủ duy nhất**. `PlayBatchMatcher` và `SixCardLeadRecovery` còn trong repo như legacy tests, không gọi ở dịch vụ thực tế.
- Trong màn hình chính chọn **TỰ KIỂM TRA NHẬN DIỆN → Làm mới bảng tự kiểm tra** để xem điểm của ba bộ đếm và từng lá tay.
- Log chẩn đoán mới có `counter_ocr` (ứng viên, điểm, biên, lý do) và `counter_rejected` (chuyển số không hợp lệ).
- Có bộ kiểm tra ảnh thật và công cụ đọc lại chuỗi ảnh/JSONL phục vụ kiểm thử liên tục.

**Kết quả trên 18 huy hiệu gắn nhãn:** 17 đúng, 1 từ chối, 0 đọc sai. Bộ mẫu còn quá nhỏ để công bố 99% chính xác; chưa test APK trên điện thoại thật. Chưa điều chỉnh AI chiến thuật.

Xem `CHANGELOG_V3_5.md` và `CHANGELOG_V3_4.md` và `DATA_CAN_THU_THAP_V3_4.md` để biết chi tiết, giới hạn, cách thu nhãn.

## Build
1. Giải nén ZIP, đưa **nội dung bên trong** vào gốc repository GitHub.
2. **Actions → Build Chặt Heo Tracker APK → Run workflow**.
3. Tải artifact `ChatHeoTracker-v3.5-hand-ledger-safety-apk` rồi cài bằng file `app-debug.apk`.

**Lưu ý:** Thay chữ ký APK (ví dụ build ở máy khác) có thể yêu cầu gỡ bản cũ và làm mất dữ liệu tự học trên điện thoại. Nên sao lưu dữ liệu huấn luyện trước.
