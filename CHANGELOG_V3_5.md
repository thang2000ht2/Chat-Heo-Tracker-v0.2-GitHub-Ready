# Chặt Heo Tracker v3.5 — sửa bảo toàn bài tay, kiểm soát xung đột lịch sử

## Bằng chứng từ log lỗi ngày 20/10/2026

- `hand_synced`: năm lá **8♣ 8♦ 9♠ 9♥ 2♥** đúng; sau khi thấy 2♥ giữa bàn, app cập nhật còn bốn lá.
- Sau đó app quét một lá 9♥ ở trạng thái quạt tưởng là hoàn chỉnh. Tại thời điểm chấp nhận, **8♣ 8♦** có dấu vết giữa bàn nhưng **9♠ chưa có**; kết quả quét này không đủ bằng chứng để giảm 4 → 1.
- Về sau giữa bàn xuất hiện cả **9♠ 9♥**, nhưng dữ liệu bài tay cũ vẫn hiển thị 9♥. Khi đồng hồ bộ đếm không nhận được (`badge_absent`), một số nước bị `unattributed_play`.

## Phần sửa (giới hạn và có thể kiểm thử độc lập)

1. **VerifiedHandTransition:** cập nhật bài tay đã xác nhận chỉ khi là tập con thực sự và **mọi lá rời tay đã có bằng chứng hình ảnh công khai**. Trường hợp không đủ bằng chứng được giữ ở trạng thái `WAIT_PUBLIC_CARDS` mà không ghi dữ liệu học đúng/sai.
2. **Phát hiện trùng giữa bàn/bài tay:** nếu lá công khai vẫn đang nằm trong bài tay theo trạng thái cũ, gắn cờ xung đột; HUD báo `bài cũ · chờ khớp`, không đưa gợi ý dựa vào tay đang mâu thuẫn, không báo tỷ lệ hàng giả như dữ liệu sạch.
3. **Không nhận nhầm nước của tôi từ nước không gán người:** `unattributed_play` vẫn cập nhật quân công khai, nhưng **không tự xóa bài tay hoặc ghi nhãn local_play_observed** khi không xác nhận được tay bài giảm. Dữ liệu chưa gán vẫn riêng biệt.
4. **Chưa đánh:** quân đã xuất hiện công khai không còn được thêm lại chỉ vì bài tay cũ vẫn ghi giữ quân đó.
5. **Giữ nguyên** bộ đếm và `MoveEvidenceFusion` v3.4, tỷ lệ 3ĐT/4ĐT/TQ có thể = 0 nếu luật/bài đã thấy loại trừ chắc chắn; không thêm trọng số AI mới.

## Kiểm thử

- `TestVerifiedHandTransition`: tái hiện 5 → 4 (được xác nhận) → 1 (chưa đủ 9♠, phải chờ), ngăn 9♥ vừa ở tay vừa trên bàn, chặn nhận quân lạ và số lá tăng.
- Tất cả test Java trước đó được giữ nguyên trong GitHub Actions.
- Bộ test ảnh, Python data/bench smoke tiếp tục được chạy trước bước build Android.

## Chưa thể cam kết

- Chưa có phép đo 99% trên bộ ảnh độc lập; thiếu log kết thúc đủ trạng thái ở các tình huống có hiệu ứng che bài.
- Mã chỉ được kiểm thử bằng Java và ảnh/chuỗi mẫu trong môi trường này, **chưa được biên dịch APK Android và thử trên Xiaomi**.
- Bản này thiên về từ chối/tạm chờ khi nghi ngờ; có thể chậm gợi ý hơn trong tình huống bị che lâu. Khi có log v3.5 sẽ đánh giá mức độ từ chối trước khi nới điều kiện.
