# Chặt Heo Tracker v2.6 — Loại trừ bài tay khỏi màu báo hàng

- Kiểm chứng v2.5: phần sampler đã loại trừ bài tay và bài đã đánh; lỗi nhìn thấy nằm ở việc tô nguyên nhóm cùng số trong danh sách “Chưa đánh”.
- v2.6 chỉ tô **từng chất** còn có thể ở phía đối thủ: **đỏ** nếu cả 4 lá cùng số đều ở ngoài bài tay và chưa lộ; **xanh** nếu có thể nằm trong 3/4 đôi thông; **xám** nếu chính bạn giữ lá đó.
- Danh sách “Chưa đánh” vẫn tính đúng mọi lá chưa đánh, bao gồm lá trong tay bạn. Thêm nhãn `ngoài tay N · TQ:X 3ĐT:Y 4ĐT:Z`, là **số bộ có thể tạo từ tập bài ngoài tay trên toàn bộ ba đối thủ**, không phải số bộ chắc chắn nằm ở một người.
- Nếu chưa xác nhận bài tay hoặc bài tay mâu thuẫn với bài đã thấy, ngưng tô đỏ/xanh và tạm hiện `cần xác nhận bài tay`; phần trăm dưới avatar để `—` thay vì dùng sai mẫu.
- Bài đã thấy trên bàn nhưng chưa xác định người đánh (`waitingCards`) cũng bị loại khỏi cảnh báo màu, tương ứng sampler.
- Chỉnh bài tay hoặc thủ công thêm/xóa lá đã đánh sẽ **vô hiệu hóa mẫu cũ và đánh dấu tính lại tỷ lệ từng đối thủ**.
- Có xét số quân còn lại của ba đối thủ: nếu tất cả còn dưới 4 thì không tô tứ quý, dưới 6 không tô 3 đôi thông, dưới 8 không tô 4 đôi thông. Bộ đếm không nhận được thì không loại cứng theo số lượng.
- Bài kiểm thử riêng xác nhận rằng giữ một quân trong tứ quý có thể có khiến tứ quý đó bất khả thi; giữ một quân trong đôi thông làm mất chuỗi tương ứng; và mẫu 3ĐT/4ĐT/TQ trở về 0 khi toàn bộ cấu trúc không còn khả thi.

**Giới hạn:** số bộ khả dĩ trong toàn bộ tập bài chưa thấy không cho biết có bao nhiêu người đang giữ chúng. Phần trăm dưới avatar vẫn chỉ là tỷ lệ lấy mẫu và chưa được hiệu chuẩn bằng ván thật. Chưa có APK kiểm thử trên máy Xiaomi.
