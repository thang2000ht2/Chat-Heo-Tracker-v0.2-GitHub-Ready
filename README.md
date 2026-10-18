# Chặt Heo Tracker v2.9 — Sửa đồng bộ, ghép lượt đánh và log chẩn đoán

**Dự án Android offline, dành cho GitHub Actions build APK.** Không có quyền INTERNET.

## Sửa lỗi dựa trên ba ảnh người dùng ngày 18/10/2026

1. **Ảnh 1 — đang cầm `J♦ J♥ K♠` nhưng J♥/K♠ biến mất khỏi Chưa đánh:** Bộ đọc ảnh thực ra đọc được đủ 3 lá; điểm nghẽn nằm ở khâu trạng thái bài tay / lịch sử. Sau ba lần quan sát được cùng bộ bài đầy đủ, tay được đồng bộ độc lập. Nếu lá đang cầm bị lịch sử ghi nhầm là đã ra, xóa liên kết lịch sử sai và làm sáng trở lại trong Chưa đánh. Khi tổng dữ liệu vẫn vượt 52, giữ bài tay để bạn nhìn thấy nhưng ẩn tỷ lệ hàng đối thủ cho tới khi đối soát hợp lệ, tránh xác suất giả.
2. **Ảnh 2 — quét đủ tám lá nhưng HUD báo chưa đồng bộ:** Cho phép xác nhận lần đầu với **ít hơn 13 lá** nếu có bằng chứng trận đã diễn ra (ít nhất một bộ đếm đối thủ dưới 13 hoặc có lượt đánh), sau ba ảnh quạt bài giống nhau. Nếu mọi người vẫn 13 và nhìn thấy tám lá thì chưa coi đó là bài tay hoàn chỉnh. Phần tứ quý 3/đôi thông sẽ tô chỉ khi đủ lá trong tập chưa đánh *ngoài tay* và bộ đếm/lịch sử cho phép.
3. **Ảnh 3 — sảnh 8♠ 9♣ 10♦ J♠ Q♥ bị xé thành lịch sử 3 lá và 2 lá:** Chờ ghép các mảnh quan sát theo một lần giảm bộ đếm **đúng 5 lá**; chỉ sau khi hợp đủ mới gán đúng một lượt cho người chơi. Nếu thiếu quân, giữ trạng thái chưa chắc chắn và ghi log, không gán sai từng phần.

## Nhật ký lỗi có thể gửi để phân tích

Trong màn hình chính Tracker chọn **Xuất log chẩn đoán lỗi quét JSONL**, lưu tên `chat_heo_scan_diagnostics.jsonl`. Hãy gửi log và một ảnh/video tương ứng nếu còn lỗi. Log có mốc thời gian, mã ván ngẫu nhiên, bài/ô đếm đã quét, mảnh lượt đánh, kết quả ghép và thông tin xung đột; không lưu màn hình, biệt danh, hoặc dữ liệu cá nhân nhận dạng. Tự xoay vòng, tối đa khoảng 3 MB, không phụ thuộc nhật ký huấn luyện. Có nút xóa riêng.

**Nhật ký chẩn đoán** dùng để debug; **dữ liệu huấn luyện JSONL** vẫn tồn tại độc lập. Những ghi nhận phải sửa do xung đột bài tay được đánh dấu không đủ tin cậy và lịch sử đối thủ trong ván hiện tại được rút lại nếu cần.

## Kiểm thử

- Các bài thuần Java, gồm ghép 3+2 lá thành 1 sảnh 5 lá, xác nhận giữa ván, quyền riêng tư của log, màu bài tay, rút lại nhãn học sai.
- Bộ nhận diện thực `HandRecognizer` và `CardRecognizer` trên các ảnh được che nhận dạng trong `test_images/`: ảnh 1 đúng `J♦ J♥ K♠`, ảnh 2/3 đúng tám lá `4♥ 7♣ 8♦ 9♥ 10♣ Q♣ K♣ A♦`, ảnh 3 đọc đủ giữa bàn `8♠ 9♣ 10♦ J♠ Q♥`.
- Đây chỉ là ảnh tĩnh và bài test thời gian mô phỏng; chưa có xác nhận luồng Android thực trên Xiaomi. APK **chưa được biên dịch trong môi trường hiện tại**.

**Giới hạn:** việc tự phân biệt sảnh đang diễn ra theo chuyển động có thể vẫn lỗi nếu video bỏ quá nhiều khung hoặc bộ đếm sai; không suy luận quân chưa nhìn thấy. Công cụ mô phỏng, luật chặt và phần trăm vẫn chưa được hiệu chuẩn bằng dữ liệu thực.
