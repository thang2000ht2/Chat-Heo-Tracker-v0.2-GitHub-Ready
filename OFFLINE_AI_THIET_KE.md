# Thiết kế AI offline – cơ sở v1.5 và nâng cấp v1.6

## Luồng dữ liệu

`MediaProjection → Recognizers → Kiểm tra đủ 52 quân → Trích lịch sử từng đối thủ → ContextualHandSampler → MonteCarloAdvisor → Hiển thị 5 giây`

- **Bài được biết chắc:** bài trên tay, các quân đã đánh, quân giữa bàn và số bài ba đối thủ.
- **Thông tin ngữ cảnh:** các lượt đánh *quan sát được* theo thứ tự của từng người, gồm đánh lẻ, đôi, bộ ba, tứ quý, sảnh; hai lượt đánh lẻ cùng số gợi ý xu hướng phá đôi.
- **Thông tin không biết:** bài úp và mục tiêu thực sự của đối thủ. Không suy ra bỏ lượt nếu chưa có tín hiệu xác thực.

## Mô phỏng 192 cách chia bài đối thủ

Bước 1: loại bỏ khỏi bộ 52 quân tất cả bài tay và bài đã đánh. Nếu tổng quân chưa biết không bằng tổng số quân của ba đối thủ thì ngưng gợi ý.

Bước 2: tạo cách phân bố các quân còn lại vào ba tay bài đúng số lượng. `ContextualHandSampler` dùng trọng số theo chuỗi lịch sử đánh (thích ra bộ/ra lẻ/ra sảnh) nhưng giới hạn hệ số và yêu cầu bằng chứng, tránh tự tin quá mức. Khi ít hơn hai lượt đánh của một người, áp dụng xác suất chia đều.

Bước 3: đối với từng nước hợp lệ đang xem xét, kiểm tra khả năng có quân/bộ chặn trong mỗi kịch bản và nhân với hệ số sẵn sàng chặn (`OpponentBehaviorModel`). Một quân J nằm trong đôi J thường được chấm ít sẵn sàng để tách ra chặn lẻ 10 hơn J lẻ, nhưng không bị loại trừ tuyệt đối.

Bước 4: cộng trung bình các kịch bản, lấy nước rủi ro thấp nhất. Khi các lựa chọn tương tự nhau mới xét tiêu chí phụ số quân xả được và mục tiêu xếp hạng.

Bước 5: hiển thị `ĐÁNH ...`, `≈x% mức chặn ước tính (offline)`; nếu cần `⚠ HEO: nguy cơ bị chặt ...` rồi ẩn sau 5 giây. Không lạm dụng UI.

## Giới hạn quan trọng

Đây là **Monte Carlo lấy mẫu có trọng số + policy heuristics một bước**, không phải ISMCTS đầy đủ; không có dữ liệu huấn luyện nhãn chặt/bỏ hay tỷ lệ thắng thật để hiệu chuẩn %. Chạy offline nhờ Java thuần, không tích hợp mô hình ngôn ngữ hay tải model. Trong tương lai có thể bổ sung self-play + học policy nhỏ trên máy tính và đóng gói trọng số offline, nhưng cần luật game chính xác, bộ mô phỏng và dữ liệu kiểm thử độc lập.

## Bổ sung v1.6: tự học theo người ngồi ghế

`SeatVisualSignature` và `SeatIdentityTracker` theo dõi ổn định vùng **tên trên dải đỏ** của ba người chơi, không dùng avatar để nhận người. Khi đổi tên/vắng ghế qua đủ nhiều khung hình, mã xóa riêng `LearnedOpponentHistory` của ghế đó. Dữ liệu của hai người còn lại được giữ nguyên.

`LearnedOpponentHistory` chỉ nhận các nhóm lá bài đã được ghép với biến động số bài của người chơi, lưu cục bộ qua các ván. Mốc ván mới được giữ để các quân cùng số ở những ván khác nhau không bị coi là phá đôi. `OpponentBehaviorModel` và `ContextualHandSampler` dùng dữ liệu học dài hạn này; mô hình không có quyền đọc bài úp, không được suy luận những lượt bỏ qua mà hệ thống chưa nhận diện được.

`MonteCarloAdvisor` trả về một nước đánh và **mức chặn ước tính tổng thể + ba mức riêng Trên/Trái/Phải**. Đây là chỉ số mô phỏng có yếu tố hành vi, không phải xác suất thực nghiệm đã hiệu chuẩn. Không có truy cập mạng, không truyền hồ sơ người chơi ra ngoài điện thoại.
