# Chặt Heo Tracker v2.0 — cảnh báo theo nước đánh và gợi ý không bị khóa bởi lịch sử thiếu

## 1. Cảnh báo heo đúng tình huống

- Không cảnh báo chỉ vì bạn đang giữ quân 2.
- **Chỉ cảnh báo nếu nước AI đang đề xuất thật sự có quân 2**, nước ấy hợp lệ theo bộ luật được mã hóa và các lượt chia bài mô phỏng cho thấy có nguy cơ bị chặt.
- Khi có dữ liệu đủ để mô phỏng, hiển thị `⚠ HEO: ≈N% mẫu có thể chặt`, dựa trên 192 mẫu phân bổ bài chưa biết và mô hình hành vi; **không phải xác suất thắng/chặt được hiệu chuẩn ngoài đời**.
- Trường hợp đối thủ đánh **đôi**, bạn chỉ có **một quân 2**: không được gợi ý đánh 2 lẻ để chặn đôi và không cảnh báo sai như trước.
- Luật chặt, thứ tự chất và tiền phạt riêng của game cần đối chiếu thể lệ chính thức trước khi dùng làm quyết định chắc chắn.

## 2. Không khóa gợi ý do số bài đối thủ hoặc lịch sử chưa khớp

- Khi bài tay đã được xác nhận, việc quạt bài tạm bị icon/chat che không tự hủy xác nhận cũ.
- Nếu bài tay hợp lệ và bộ trên bàn được nhận diện, nhưng số bài đối thủ chưa đọc đủ hoặc lịch sử thiếu, app đưa ra **một gợi ý chiến thuật tạm** để giảm số lượt cần về bài.
- Gợi ý tạm hiển thị `Chặn: chưa đủ dữ liệu (ước tính tạm)`; **không bịa số phần trăm**.
- Nếu bài tay chưa từng xác nhận, chồng lấn với bài đã đánh hoặc bài giữa bàn sai/không rõ, yêu cầu quét lại để tránh khuyến nghị sai.

## 3. Tối ưu nhãn Quét

- Đổi nội dung dài (`13 lá · chưa đối soát`, `13 lá · thiếu lịch sử 4`) sang `13 lá · lệch`, `13 lá · thiếu 4`.
- Mở rộng nhãn từ 136dp lên 182dp, điều chỉnh cỡ chữ để giảm cắt cụt; vẫn nằm sát nút Gợi ý góc trái.

## 4. Kiểm thử

- Thêm `TestHeoContextAndFallback` cho đối thủ đánh đôi, người chơi giữ heo lẻ, phần trăm cảnh báo chỉ khi *đánh* heo và gợi ý tạm khi thiếu bộ đếm.
- Chạy toàn bộ kiểm thử pure-Java bằng GitHub Actions trước khi build APK.
- **Chưa biên dịch hoặc kiểm tra ứng dụng trên Xiaomi 17 Pro Max trong môi trường này.**
