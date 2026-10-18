# Chặt Heo Tracker v2.8 — Flexible Hand Scanner · Owned Cards Dimmed

Project Android 16 chạy offline; build qua GitHub Actions workflow `.github/workflows/build-apk.yml`.

**Mới trong v2.8:** danh sách `Chưa đánh` giữ tất cả quân chưa ra bàn nhưng **làm mờ quân đang ở bài tay đã xác nhận**, chỉ tô đỏ/xanh hàng có thể của đối thủ. Bộ quét quạt bài mới xử lý quân được nâng lên, không đòi dải trắng của mọi lá cùng một chiều cao. Bộ đối soát có thể sửa riêng quân bị lịch sử đánh nhầm nếu bài tay hiện tại được đọc đầy đủ **ổn định ba khung hình**, đồng thời ghi log xung đột.

**Kiểm thử:** test Java thuần cho state, test Java chạy lớp `HandRecognizer` với ảnh bài đã ẩn danh (5 lá chọn lên và 10 lá trải đều), Python tests từ các bản trước. Test không thay thế kiểm thử APK trên điện thoại.

**Chưa làm:** mạng nhận diện được huấn luyện, xác minh luật Pig Hunters và mô hình chặt heo đã hiệu chuẩn. Không lưu màn hình thô hay tải ảnh lên mạng. Vui lòng xem `CHANGELOG_V2_8.md`, `GITHUB_BUILD_HUONG_DAN.md`.
