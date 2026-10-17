**Lưu ý v2.3:** Bản này khắc phục báo cáo tỷ lệ chặn thiếu hiệu chuẩn và bổ sung bộ kiểm thử tự đấu luật tham chiếu. Chưa huấn luyện AI từ ván thật, chưa có engine Pig Hunters hoàn chỉnh. Đọc README_V2_3.md.

# Chặt Heo Tracker v2.1 — Tempo, HUD & Redeal

Đây là mã nguồn **thử nghiệm** cho Android 16, chạy nhận diện màn hình và bộ gợi ý chiến thuật **offline**. Mô hình không tự đánh bài. Tỷ lệ chặn là ước lượng dựa trên mẫu phân chia bài đối thủ và hành vi quan sát, không phải xác suất đã được chứng minh trên ván thực.

## Cập nhật v2.1

- Không ẩn `Tôi / Chưa đánh` khi lời gợi ý xuất hiện.
- Bộ gợi ý giữ A/2 ở đầu ván khi chưa cần tranh lượt, ưu tiên xử lý quân lẻ thấp trước, đồng thời cân nhắc cấu trúc bài, xu hướng đối thủ và mức nguy cơ bị chặn.
- Bộ phát hiện ván mới quan sát 13/13/13 trong 7 khung hình **sau khi ván cũ đã có tiến triển**, không reset lặp khi vào phòng lần đầu.
- Dữ liệu học đối thủ lưu xuyên ván như bản trước; không xóa khi tự reset ván.
- 18 nhóm kiểm thử Java gồm hai bài mới cho nhịp đánh và tự reset.

Chi tiết: [CHANGELOG_V2_1.md](CHANGELOG_V2_1.md).

## Build qua GitHub

1. Giải nén ZIP, tải toàn bộ file **bên trong** lên repository, giữ `.github/workflows/build-apk.yml`.
2. Mở **Actions → Build Chặt Heo Tracker APK → Run workflow**.
3. Khi build thành công, tải artifact `ChatHeoTracker-v2.1-tempo-redeal-apk`, giải nén để lấy `app-debug.apk`.

**Chưa được kiểm chứng trực tiếp trên Xiaomi** vì môi trường tạo bản này không có Android SDK để build APK. Nếu bạn gặp lỗi, gửi log GitHub Actions hoặc video quay một lượt chuyển ván cùng số bài cạnh các đối thủ.

## Lưu ý dữ liệu tự học

APK `debug` từ GitHub Actions có thể được ký bằng khóa mới mỗi lần chạy. Để cập nhật mà không mất dữ liệu ứng dụng, cần dùng **cùng khóa ký** qua các lần biên dịch. Nếu buộc gỡ app cũ vì lỗi chữ ký, dữ liệu tự học cục bộ có nguy cơ bị xóa; hiện chưa có chức năng xuất/nhập dữ liệu.

## V2.2 – Thu thập dữ liệu huấn luyện offline

Xem [DATASET_TRAINING_GUIDE_VI.md](DATASET_TRAINING_GUIDE_VI.md) và [CHANGELOG_V2_2.md](CHANGELOG_V2_2.md). Mở app để bật/tắt lưu, ghi hạng ván vừa rồi, xuất JSONL hoặc xóa nhật ký. Mọi kết quả hạng đều phải do người dùng xác nhận; ảnh màn hình không được lưu.
