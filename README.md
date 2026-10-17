# Chặt Heo Tracker v1.7 — Offline AI / HUD & Scanner Recovery

Dự án mã nguồn Android dùng cho game Tiến Lên – 13 – Pig Hunters (giao diện đã cung cấp). **Đây là mã nguồn thử nghiệm để build APK**, chưa được kiểm chứng trực tiếp trên Xiaomi 17 Pro Max. Thuật toán chiến thuật không phải mạng nơ-ron đã huấn luyện và không cam kết tỷ lệ thắng.

## Những thay đổi so với v1.6

- **Thông báo quét và gợi ý ở gần vùng bài tay:** đặt tại phần giữa-phía dưới (khoảng 30% chiều rộng và 55% chiều cao màn ngang), không nằm đè bảng lịch sử đối thủ bên trái. Gợi ý tự ẩn sau 5 giây; lớp gợi ý không chặn chạm.
- **Bỏ nút “Bài tay” trên lớp phủ.** Nếu OCR sai, có thể sửa bài tay ở màn hình chính Tracker (lệnh lưu mới có hiệu lực với service đang chạy).
- **Xếp hạng 1–2–3–4 ưu tiên về sớm:** dùng số lượt đánh tối thiểu *ước lượng* sau nước hiện tại làm trọng số chính; bổ sung rủi ro bị chặn, chặt heo và mức nguy hiểm khi đối thủ còn ít quân. Không còn mặc định chỉ ưu tiên chọn lá khó bị chặn nhất.
- **Đỡ tốn thời gian quét:** vẫn xử lý bài giữa bàn và số lá đối thủ trên các khung hình lấy mẫu, nhưng chỉ kiểm tra dấu hiệu tên theo chu kỳ 4 khung, bài tay được ưu tiên khi thiếu/chưa khớp và quét theo chu kỳ 2 khung khi đã có bài. Thử dùng các quân mới thấy nhưng chưa kịp gán người chơi để đối soát 52 lá, không suy diễn quân bị che kín.
- **Khôi phục khi quét bị đứng:** khi luồng ảnh không còn đến hoặc không giải mã xong trong hơn 7 giây, thử nối lại `ImageReader` với `VirtualDisplay` đang tồn tại, tối đa một lần mỗi 15 giây. Nếu quyền MediaProjection bị hệ thống hủy, phải vào Tracker để cấp lại (Android 16 không cho tự cấp lại quyền). Bấm giữ nút ↻ trong game để **Quét lại, giữ lịch sử**; chạm ↻ vẫn đặt lại toàn bộ dữ liệu của ván.
- **Tự học không mất khi người rời phòng:** lưu tối đa 48 hồ sơ dựa trên dấu hiệu tên đã xác nhận và 100 mục gần nhất mỗi hồ sơ. Khi người rời đi chỉ đổi phiên hiện tại, **các quân đã đánh trong ván và kho lịch sử học vẫn còn**. Nếu người quen quay lại cùng dấu hiệu tên, app có thể phục hồi mô hình cũ. Mọi dữ liệu lưu trên máy; không có quyền Internet. Đây là dữ liệu để điều chỉnh mô hình quy tắc và phát triển huấn luyện về sau, **không phải mô hình ML tự train**.

## Cách biên dịch trên GitHub

1. Giải nén ZIP và tải tất cả nội dung (kể cả `.github/workflows/build-apk.yml`) lên repository (ưu tiên Private).
2. Mở **Actions → Build Chặt Heo Tracker APK → Run workflow**.
3. Chờ tất cả kiểm thử và bước `Build debug APK` hoàn thành.
4. Vào **Artifacts** tải `ChatHeoTracker-v1.7-stable-hud-offline-apk`, giải nén lấy `app-debug.apk` và cài thử trên Xiaomi.

## Kiểm thử thực tế nên làm

- Bắt đầu ván mới, kiểm tra nút reset chạm ngắn và **giữ lâu** cho quét lại mà không xóa dữ liệu.
- Khi đến lượt, kiểm tra gợi ý hiện ở giữa-thấp, không đè lịch sử đối thủ bên trái, và tự ẩn sau 5 giây.
- Kiểm tra sau khi người bên trái thoát/đổi người: lịch sử **ván hiện tại** không biến mất, kho hồ sơ học vẫn còn khi mở app lại.
- Quan sát tình huống OCR thiếu lá: Tracker phải hiển thị tình trạng thiếu/đề nghị quét lại chứ không tự đoán bài úp.
- Nếu app vẫn ngừng nhận diện dù quét lại, mở Tracker cấp lại quyền ghi màn hình, gửi video mới và log GitHub/Android để xác định nguyên nhân.

## Giới hạn

Nhận diện dựa trên mẫu ảnh của những video đã cung cấp; tốc độ thực tế phụ thuộc độ phân giải, hiệu ứng game, lớp chat và giới hạn MediaProjection. Logic nhận diện **không thể khôi phục những lá đã biến mất khỏi bàn mà trước đó chưa bao giờ quan sát rõ**. Thuật toán phân loại bộ chặt, quy tắc tiền phạt và xác suất hạng chưa được kiểm định độc lập theo luật cụ thể của game.

**Ghi chú cảm ứng Android:** HUD và popup gợi ý sử dụng `FLAG_NOT_TOUCHABLE` và alpha 0.78 (dưới ngưỡng 0.8 áp dụng cho lớp phủ không tin cậy trên Android 12+). Khi quét/gợi ý, bảng dưới được tạm ẩn để không chồng nhiều cửa sổ, sau đó xuất hiện lại.
