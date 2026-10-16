# Lộ trình huấn luyện AI cho Chặt Heo Tracker

## Trạng thái trung thực của v1.1

**Chưa có mạng nơ-ron được train.** Bản v1.1 gồm thuật toán phân rã bài chính xác (minimum-turn partition) và Monte Carlo sampling **192 bộ bài úp giả định** khi dữ liệu tổng hợp khớp 52 lá. Đây là bản **heuristic + mô phỏng nhanh**, không phải ISMCTS đầy đủ, không phải một agent đã đánh thắng người thật và cũng không đưa ra xác suất thắng đã kiểm định.

Nút **Gợi ý** chỉ hoạt động khi người dùng bấm. App không chạm game, không chọn bài hộ, không đọc dữ liệu nội bộ game.

## Mẹo và kiến trúc thực tế nên làm khi muốn training

### Giai đoạn A: làm bộ luật chạy được và khóa phiên bản luật

1. Xin ảnh/clip các trang **thể lệ game Chặt Heo – 13 Pig Hunters**, đặc biệt: thứ tự chất, luật đè đôi/sảnh, ba/bốn đôi thông chặt gì, tứ quý chặt gì, yêu cầu 3♠ mở đầu, xử lý bỏ lượt, tới trắng và hệ số phạt/thưởng.
2. Viết hàm `getLegalMoves(state, player)` và `step(state, action)` độc lập với Android. Đảm bảo không có nước bất hợp lệ; kiểm thử bằng các ví dụ đã xác minh trong game.
3. Đơn vị reward: `0.5 * normalized_first_place + 0.5 * normalized_net_payout`, trong đó payout thật phải lấy từ bảng luật. Chuẩn hóa mỗi thành phần để tiền phạt lớn không áp đảo xác suất về nhất. Có thể thử thêm mô hình lợi ích khác, nhưng phải đánh giá chéo.
4. Không lấy 52 lá chưa xuất hiện trên màn hình làm bài của một đối thủ. Bài úp phải là **phân phối xác suất**, và số lượng bài úp phải khớp bộ đếm từng người.

### Giai đoạn B: tạo dữ liệu chất lượng

- Tạo **self-play** bằng engine luật, bắt đầu từ bộ đối thủ khác nhau: đánh thấp nhất, xả được nhiều lá, giữ heo/hàng, gây áp lực khi đối thủ ít bài, ngẫu nhiên hợp lệ. Dùng *opponent pool* gồm checkpoint cũ để tránh AI học cách thắng chỉ một kiểu đối thủ.
- Mỗi mẫu học nên chứa **information set**: 13 lá của mình (hoặc phần còn lại), lượt/nhịp hiện tại, tập lá công khai đã đánh, số lá còn lại đối thủ, lịch sử bỏ lượt/đè, tình huống bị chặn, luật và thông tin ván. **Tuyệt đối không đưa bài úp thật của đối thủ vào observation đầu vào của mô hình.** Bài úp thật chỉ dùng cho simulator để tính kết quả cuối.
- Thay đổi chỗ ngồi, người đi đầu và seed chia bài; log bản luật và phiên bản mô hình. Chia train/validation/test theo **toàn ván**, không chia ngẫu nhiên theo khung hình của cùng một ván (rò rỉ dữ liệu).
- Không hoán vị chất mù quáng để tăng dữ liệu: thứ tự ♠ ♣ ♦ ♥ ảnh hưởng giá trị quân trong Tiến Lên.
- Tạo thêm dữ liệu OCR bị che chat/emote, chuyển động, màu nền, rung/scale; không sửa nhãn lá bị che toàn bộ theo suy đoán.

### Giai đoạn C: thuật toán

1. **Baseline mạnh:** DP ít lượt xả bài (đã có ở v1.1), so với baseline người chơi đơn giản. Đánh giá *legal move rate* = 100% và chi phí runtime.
2. **Belief sampler:** sinh nhiều bộ bài đối thủ **phù hợp số lượng và lịch sử public**. Bỏ lượt không có nghĩa không có quân chặn; có thể dùng trọng số hành vi thay vì cấm tuyệt đối cấu hình bài.
3. **Information Set Monte Carlo Tree Search** / self-play, thay vì chỉ 'mở' bài úp và giải một bàn giả định. Cần xử lý sai lệch do **strategy fusion** khi determinize.
4. **Train neural policy/value** từ replay tự chơi + search teacher; cân nhắc behavioral cloning ban đầu, sau đó league self-play. Với Android, export model nhỏ (TFLite/ONNX nếu cần), đo pin/CPU/RAM và thời gian gợi ý trên Xiaomi Android 16.
5. **Reward tuning:** không lấy tỉ lệ chặn hay số bài đánh được làm mục tiêu duy nhất; dùng độ lợi tiền + khả năng về nhất; học trên nhiều luật phòng và kiểm tra `value function` tránh exploitable policy.

### Giai đoạn D: benchmark và kiểm định

- Đánh **ít nhất hàng nghìn ván ngoài tập train**, đổi seed/bàn/người bắt đầu, so với các baseline và self-play checkpoint trước. Báo mean, khoảng tin cậy bootstrap và sai số theo vị trí bàn.
- Báo riêng **tỉ lệ về nhất, tiền lời/phạt trung bình (theo điểm quy đổi), tỉ lệ bị chặt heo, bị cóng, lượt cần ra hết bài, độ trễ một lần gợi ý, sai số nhận diện**.
- Đánh giá khả năng chống OCR lỗi: cố ý bỏ sót/che 10–30% lượt đánh, tăng dữ liệu không chắc chắn; agent phải **từ chối** khi thiếu thông tin quan trọng thay vì tự tin bịa quân.
- Đánh giá mô hình hiệu chỉnh xác suất (calibration): ví dụ nhóm tình huống AI bảo thắng ~60% có thực sự thắng xấp xỉ 60% không; nếu không thì không hiển thị phần trăm thắng.
- Làm A/B offline trước khi gợi ý trực tiếp. Không sử dụng dữ liệu cá nhân hay ghi màn hình để train từ xa nếu người dùng chưa đồng ý.

## Vì sao v1.1 không thể 'AI train tốt nhất' ngay

- Chưa có **engine luật Chặt Heo hoàn chỉnh, được kiểm chứng**.
- Chưa có thể lệ tiền phạt/chặt heo để tính phần thứ hai của reward.
- Chưa có tự động nhận diện **bài trên tay**, phải nhập và cập nhật bằng tay.
- Chưa có dữ liệu đánh giá độc lập theo hàng nghìn ván; kết quả tự test trong cùng video chỉ là kiểm thử phần mềm.
- Giới hạn chính xác: bộ nhận diện và bộ đếm có thể bị chat/icon che; 192 mẫu ngẫu nhiên **không** là độ tin cậy đã hiệu chỉnh.

## Tài liệu tham khảo

- Cowling, Powley & Whitehouse (2012), Information Set Monte Carlo Tree Search: https://eprints.whiterose.ac.uk/id/eprint/75048/1/CowlingPowleyWhitehouse2012.pdf
- Whitehouse et al. (2011), Determinization and ISMCTS for Dou Di Zhu: https://doi.org/10.1109/CIG.2011.6031993
- Frank & Basin (2001), Search in imperfect-information games: https://doi.org/10.1016/S0304-3975(00)00083-9
- Một mã nguồn tham khảo engine Tiến Lên (không sử dụng/nhúng code): https://github.com/vxcozy/tien-len
