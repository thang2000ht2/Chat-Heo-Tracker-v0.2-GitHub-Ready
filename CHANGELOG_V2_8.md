# Chặt Heo Tracker v2.8 — dim owned cards + flexible screenshot scanning

## Thay đổi

1. **Bài tay đang giữ được làm mờ** trong danh sách `Chưa đánh`: từng chất thuộc tay đã xác nhận dùng màu xám mờ và kích thước chữ nhỏ hơn 8%; nếu tất cả các chất còn lại của một số đều nằm trong tay tôi thì cả số đó cũng mờ. Màu đỏ (tứ quý có thể) / xanh (đôi thông có thể) chỉ dùng cho lá ngoài tay. Chú thích `[mờ: tay tôi]` xuất hiện nếu bài tay đủ tin cậy.
2. **Nhận diện quạt bài xòe/nhấc lá:** thêm `HandFanGeometry.fromWhiteEdges`, nhận dạng các mép thẻ từ nhiều dòng ảnh thay vì chỉ đòi một dải trắng liền mạch; khi quạt bị tách/che, dò lại góc số + chất của **từng lá** theo vị trí dọc độc lập (`HandRecognizer.recognizeRaisedCorners`). Vẫn dùng ngưỡng khớp, không tự đoán lá mất và cần ba khung hình giống nhau để sửa tay.
3. **Đối soát lịch sử sai:** `ObservedHandReconciler` kiểm tra tay bài đầy đủ từ nhiều khung, cho phép ô đếm một người thắng bị mất (-1), xử lý xung đột do lịch sử từng ghi quân đang trên tay là đã ra, rồi bỏ riêng bản ghi xung đột; lưu nhật ký `hand_history_conflict` để không coi lịch sử sửa như nhãn hành vi chắc chắn.
4. **Tính lại phân tích đối thủ** sau khi bộ tay được xác nhận/cập nhật để tỷ lệ hàng và màu đi cùng trạng thái mới.
5. **Trạng thái quét:** khi đã đọc được bài tay nhưng bộ đếm thắng biến mất, phân biệt `✓ tay · thiếu đếm` với lỗi không đọc được bài.

## Kiểm tra bằng ảnh thật

Hai ảnh được người dùng cung cấp đã được **ẩn toàn bộ phần trên và hai bên màn hình, chỉ giữ vùng bài tay** trong `test_images/`:

- `selected_five.png`: `4♦ 9♥ 10♥ J♣ K♥` được bộ `HandRecognizer` của ứng dụng xác định đầy đủ (5/5); ba lá giữa được nhấc lên.
- `spread_ten.png`: `4♠ 7♦ 7♥ 10♥ J♦ Q♠ K♠ K♦ K♥ A♦` được nhận đủ (10/10).

Workflow GitHub dùng Java stubs (`test_android_stubs`) để chạy **chính mã nhận dạng Android** trên hai ảnh crop. Đây là test trên hai mẫu, chưa phải đánh giá độ chính xác trên nhiều thiết bị.

## Giới hạn và rủi ro còn lại

- Vẫn sử dụng **template rank/suit thủ công**, không phải mạng neural đã huấn luyện. Chưa tối ưu khi game đổi font, skin, độ phân giải khác, hoặc lá bài bị emote/chat che nặng. Muốn nâng tiếp, cần bộ ảnh góc lá đã gắn nhãn để huấn luyện bộ phân loại TFLite offline.
- Nếu không từng nhận được tay đầu ván 13 lá, app vẫn thận trọng không coi quạt 5 lá nhìn thấy là chắc chắn đủ nếu thiếu chứng cứ đó là tay bài hiện tại.
- Lịch sử bị sửa có thể khiến một vài sự kiện đã dùng để thống kê hành vi trước đó không còn chuẩn; cần loại sự kiện xung đột khi xuất dữ liệu để huấn luyện.
- Chưa build APK thực tế hoặc chạy trên Xiaomi 17 Pro Max. Không tuyên bố tự động sửa được mọi lỗi quét từ 2 ảnh mẫu.
