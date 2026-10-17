# Chặt Heo Tracker v2.5 — các dấu hiệu giữ hàng và giới hạn thống kê

## Các thay đổi áp dụng được vào Android

- Bảng dưới từng ghế luôn có ba mục `3ĐT`, `4ĐT`, `TQ`. Ban đầu là `—` nếu chưa đọc được số quân; khi biết ba bộ đếm và phân bố lá hợp lệ, bảng hiển thị **% xuất hiện trong các bộ bài úp được lấy mẫu**, làm tròn theo bước 5%.
- Khác v2.4, **không khóa tính toán chỉ vì lịch sử thiếu quá 4 lá**. Nếu còn lá chưa xác định là ở tay bạn hay đã đánh: lấy mẫu phân bổ các quân chưa biết cho các ghế và để các quân dư ở nhóm không xác định, dán nhãn `ước lượng yếu`.
- Dùng 1.024 kịch bản khi các bộ đếm bình thường và 1.536 kịch bản khi có bất kỳ đối thủ nào còn 5 lá trở xuống. Tính ở luồng nền, không chạy hàng nghìn mẫu trong chuỗi đọc ảnh.
- Một lần giảm số bài được đọc ổn định, một lượt bài được gán, hoặc một lượt bỏ do người dùng xác nhận sẽ đánh dấu mô hình cần tính lại. Thời gian chờ giữa các lần cập nhật tối đa khoảng 1,1 giây trong trường hợp thông thường, khoảng 0,3 giây ở cuối ván (chưa tính thời gian xử lý trên máy).
- Chỉ ghi nhận lượt bỏ khi người chơi xác nhận trong Tracker, không coi khoảng trống trên màn hình là một lần bỏ lượt. Bỏ lượt **không chứng minh** không thể chặn: mô hình chỉ giảm trọng số của các giả thuyết có quân chặn và giảm hiệu lực bằng chứng sau khi đối thủ đánh thêm.
- Nhật ký JSONL lưu sự kiện `threat_sample` riêng với `opponent_move`/`opponent_pass`. Tuyệt đối không dùng giá trị dự đoán làm nhãn đúng để train.
- Công cụ `training_tools/train_move_patterns.py` được mở rộng để phân biệt các lượt **3 đôi thông và 4 đôi thông đã đánh được xác nhận**. Nó chỉ học **tần suất lựa chọn loại nước**, chưa học xác suất đang giữ bộ bài úp hoặc chặt heo.

## Dấu hiệu suy luận hữu ích (và giới hạn)

| Quan sát | Điều chỉnh hợp lý | Không được kết luận |
| --- | --- | --- |
| Đối thủ chỉ còn dưới 6 lá | 3 đôi thông không còn khả thi | Không có quân 2 hoặc tứ quý |
| Dưới 8 lá | 4 đôi thông không còn khả thi | Không có 3 đôi thông |
| Dưới 4 lá | Tứ quý không còn khả thi | Không thể có đôi |
| Một giá trị đã lộ ít nhất 1 lá ngoài tay đối thủ | Không thể có tứ quý **giá trị đó** | Không có bất kỳ tứ quý nào |
| Đã xác nhận bỏ lượt trước một quân cao | Giảm nhẹ các giả thuyết đối thủ có bài sẵn sàng chặn | Chắc chắn không có quân lớn hơn |
| Hay đánh cả đôi/bộ | Điều chỉnh **hành vi sử dụng** bài, không điều chỉnh tỷ lệ tự nhiên được chia đôi ở ván mới | Chắc chắn đang giữ hàng |
| Hay bỏ lượt, rồi chặt heo ở một ván đã xác nhận | Có thể giúp huấn luyện xu hướng **giữ và sử dụng hàng** sau nhiều ván | Họ đang có hàng ở ván kế tiếp |

## Vì sao đây chưa phải AI đã được huấn luyện

Độ phổ biến một tổ hợp trong 1.536 bộ bài chia giả lập không phải là xác suất người thật đang giữ tổ hợp đó. Phân bố còn phụ thuộc chính xác dữ liệu nhận diện, quy tắc chơi, và khả năng đối thủ chọn đánh/bỏ. Một người đánh tứ quý ở một ván **không làm họ dễ được chia tứ quý hơn** ở ván tiếp theo; lịch sử chỉ hỗ trợ mô hình dự đoán phong cách sử dụng hàng nếu họ có.

Muốn hiển thị xác suất chặt heo thật cần nhãn ván thực: lúc có cơ hội chặt, người chơi có chặt hay không; phải tách `không thể chặt` với `có thể chặt nhưng quyết định giữ`. Sau đó mới hiệu chuẩn theo từng đối thủ/giai đoạn bằng Brier score, ECE, reliability, chia train/test theo ván/người/thời gian. Các sự kiện `threat_sample` chỉ là **dự đoán**; nhãn không được suy ngược từ chúng.

## Tài liệu tham khảo nghiên cứu

1. Cowling, Powley & Whitehouse (2012), Information Set Monte Carlo Tree Search. https://eprints.whiterose.ac.uk/id/eprint/75048/
2. Southey et al. (2005), Bayes' Bluff: Opponent Modelling in Poker. https://webdocs.cs.ualberta.ca/~mbowling/papers/05uai.pdf
3. Luo & Tan (2024), Improved learning efficiency of deep Monte-Carlo for complex imperfect-information card games (Big2, DouDiZhu). https://www.sciencedirect.com/science/article/pii/S1568494624003193
4. Nhà phát hành Pig Hunters mô tả thứ tự quân và luật trò chơi trên Google Play, nhưng tài liệu trực tuyến không đủ xác thực các biến thể chặt riêng của ứng dụng: https://play.google.com/store/apps/details?id=com.pitaya.chatheo

## Các giới hạn thực thi hiện tại

- Chưa dùng nhận diện tự động nút/hiệu ứng `BỎ LƯỢT` (chưa đủ ảnh có nhãn).
- Chưa dùng biểu đồ Bayesian posterior được hiệu chuẩn từ ván thật. Trọng số likelihood lượt bỏ là mức **giả định nhỏ** được dán nhãn, không phải số đã ước lượng thống kê.
- Chưa xác minh luật chặt chồng, tứ quý chặt đôi heo hoặc phạt/thối heo đúng Pig Hunters.
- Chưa thử trực tiếp trên Xiaomi 17 Pro Max / Android 16; GitHub Actions phải biên dịch trước khi xem là APK chạy được.
