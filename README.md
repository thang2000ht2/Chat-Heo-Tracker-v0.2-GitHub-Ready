# Chặt Heo Tracker v2.0 — Context Warning / Stable Advice

**Cập nhật v2.0:** [CHANGELOG_V2_0.md](CHANGELOG_V2_0.md)

# Chặt Heo Tracker v1.8 – Scan Diagnostics / Compact HUD

Bản mã nguồn Android thử nghiệm, chạy tính toán trên điện thoại, không có API Internet. Cập nhật từ **v1.7**.

1. **Quét tay bài:** tìm dải trắng ở nhiều hàng, thử dịch tâm vị trí ± vài pixel thay vì cố định đúng y=715; chỉ ghi bài có mặt/chất nhận diện chắc và khớp qua nhiều khung hình.
2. **Nhãn Quét cạnh Gợi ý:** xuất hiện ở góc trái; không còn khung thông báo quét giữa bàn. Nhãn ngắn báo lỗi riêng cho luồng hình, hình quạt bài và bài chưa đồng bộ.
3. **Gợi ý offline:** thêm đánh giá dạng bài còn lại sau khi đánh cùng Monte Carlo/đối thủ; gợi ý không tự đánh, tự ẩn sau 5 giây.
4. **Reset:** chạm ↻ để bắt đầu dữ liệu ván mới, giữ ↻ để làm mới bộ nhận diện nhưng không xóa lịch sử. Nếu hết quyền MediaProjection, cần vào Tracker cấp lại.
5. **Dữ liệu tự học:** vẫn lưu hồ sơ đối thủ trong máy khi họ rời phòng; không xóa kho lịch sử cũ.

## Biên dịch

- Giải nén ZIP vào repository GitHub, giữ thư mục ẩn `.github/workflows/`.
- Actions → Build Chặt Heo Tracker APK → Run workflow.
- Artifacts: `ChatHeoTracker-v1.8-scan-fix-compact-ai-apk` → `app-debug.apk`.

**Độ tin cậy:** Kiểm thử bộ luật Java, phát hiện quạt bài trên dữ liệu ảnh mẫu, kiểm tra cú pháp Java. Chưa biên dịch Android SDK hoặc thử Xiaomi thực tế. Số % chặn là ước lượng từ mô phỏng, không phải tỷ lệ đã được đối chiếu bằng dữ liệu thật.

## Nếu còn không đọc được bài

1. Nhìn nhãn nhỏ bên cạnh Gợi ý: `mất khung hình` = quyền/luồng MediaProjection; `không thấy quạt bài` = giao diện/tọa độ khác ảnh mẫu; `n lá` = đọc được một phần; `lệch dữ liệu` = bài tay/lịch sử/số bài đối thủ chưa khớp.
2. Gửi ảnh chụp cả màn hình **khi vẫn bật Tracker**, tốt nhất lúc mới chia 13 lá. Ảnh này giúp căn đúng HUD và tọa độ quạt bài đang chạy trên máy thật.
3. Nếu cần log kỹ thuật: bật USB debugging trên Android và chạy `adb logcat -d -s Tracker TrackerHand` từ máy tính. Không gửi thông tin nhạy cảm có trong log khác.


## v1.9
See [README_V1_9.md](README_V1_9.md) for scanner recovery, partial public history, and independent heo warning.
