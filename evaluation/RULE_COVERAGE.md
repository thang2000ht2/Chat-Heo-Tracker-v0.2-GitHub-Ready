# Phạm vi luật của engine thử nghiệm (KHÔNG khẳng định đúng Pig Hunters)

**Đã kiểm thử bằng mã Python độc lập:** phân loại lẻ/đôi/sám/tứ quý/sảnh (không có 2), 3 đôi thông, 4 đôi thông; chặn cùng họ bài; nhóm chặt đặc biệt theo cấu hình; yêu cầu mở 3♠ trong bộ luật tham chiếu; vòng người chơi, bỏ lượt, hạng 1–4; không tạo hai lá trùng nhau trong một bộ.

**Chưa được xác minh với chính game:** các trường hợp chặt chồng và thứ tự chặt trong mọi bối cảnh, quyền chặt ngoài lượt, hồi vòng khi người vừa ra hết bài, tới trắng, đền bài, thối heo, tiền chặt/tăng phạt, chế độ bài riêng. Nếu bật `instant_win=True` hoặc `allow_out_of_turn_chop=True`, chương trình từ chối chạy thay vì giả định đã hỗ trợ.

**Một số bài trên mạng khác nhau về luật tứ quý chặt đôi heo.** Đây là lý do `Rules` có công tắc. Cần chụp màn hình luật của Pig Hunters và tạo ca kiểm thử theo đúng luật đó trước khi dùng engine làm bộ quyết định chính. Chỉ dùng engine này để so sánh baseline kỹ thuật.

**Mô hình bỏ lượt:** trong tự đấu, bot bỏ khi không có nước hợp lệ (đơn giản hóa phi thực tế). Bỏ lượt trong ván thật phải ghi được bằng hình ảnh hoặc người chơi xác nhận. Dù xác nhận đối thủ đã bỏ, không được loại hẳn khả năng họ có quân chặn: họ có thể chủ động giữ bộ. Hiện app Android không nhận diện đáng tin cậy nhãn BỎ LƯỢT nên **chưa tự ghi `opponent_pass`**. Công cụ `evaluate_results.py` chỉ cho phép sự kiện pass được xác nhận với `confidence=visual_confirmed` hoặc `user_confirmed` và có `lead`.

**Về AI:** các bot trong `selfplay.py` là heuristic, không phải ISMCTS/self-play training. Chưa dùng kết quả tự đấu để thay đổi thuật toán Android.
