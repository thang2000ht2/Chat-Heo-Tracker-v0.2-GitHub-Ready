# Changelog v1.7

- Chuyển vị trí popup quét/gợi ý sang khu vực bài tay để giữ tầm nhìn vùng đối thủ trái.
- Xóa nút Bài tay trên HUD; giữ chỉnh bài tay trong màn hình Tracker để cứu dữ liệu khi ảnh lỗi.
- Ưu tiên đường về ngắn hơn ở chế độ 1–2–3–4, tính thêm rủi ro bị chặn/chặt heo.
- Giảm số lần đọc khu vực tên và bài tay khi dữ liệu đã ổn định.
- Quét lại không mất lịch sử bằng cách GIỮ nút ↻; nút chạm ngắn vẫn reset ván.
- Tự động nối lại `ImageReader` khi luồng ghi màn hình bị đứng; xin cấp lại quyền nếu Android chấm dứt capture.
- Lưu các mẫu học vào kho lịch sử offline, không xóa khi đối thủ rời phòng. Giới hạn lưu có chủ đích và có quyền xóa thủ công.
- Kiểm tra bộ bài bằng cả quân đã xác nhận và quân mới nhìn thấy nhưng đang chờ ghép người đánh.
- Giảm độ mờ window của HUD không nhận chạm xuống 0.78; tạm ẩn bảng dưới lúc gợi ý để tránh chặn thao tác do nhiều lớp phủ chồng lên nhau (Android 12+).
