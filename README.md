# Chặt Heo Tracker v1.4 – Gợi ý tự động theo hành vi đối thủ

**Trạng thái:** Mã nguồn thử nghiệm cho Android 16, không phải APK đã kiểm chứng trên Xiaomi. Ứng dụng chỉ xử lý hình ảnh người dùng cho phép chia sẻ màn hình, không đọc dữ liệu nội bộ hay thao tác thay người chơi.

## Cách hoạt động

1. Nhận biết tới lượt bạn khi nút **ĐÁNH** màu xanh của game xuất hiện ổn định ít nhất hai lần lấy mẫu. Hiện ngay dòng `ĐANG QUÉT BÀI…`.
2. Sau ít nhất ba khung hình và khi đủ dữ liệu, tự tính gợi ý trên luồng nền, **một lần mỗi lượt**. Nếu màn hình chưa ổn định, quét thêm tối đa khoảng 3 giây. Nút **Gợi ý** dùng để quét lại thủ công khi cần.
3. Kết quả chỉ gồm một nước `ĐÁNH ...`, `BỎ LƯỢT` hoặc yêu cầu `QUÉT LẠI`, kèm một dòng **mức chặn ước tính** và cảnh báo heo (nếu có). Kết quả tự ẩn sau **5 giây** hoặc khi phát hiện bạn mất lượt.
4. Trước khi gợi ý, đối chiếu bài tay, lịch sử lá đã đánh và số bài còn lại của ba đối thủ cho tổng **52 lá**. Nếu dữ liệu sai/thiếu, không đoán bài bị che. Có thể bấm **Bài tay** để kiểm tra hoặc sửa khi cần.

## Mô phỏng hành vi (chưa phải AI được huấn luyện)

- Sinh **192 cách chia bài chưa biết** cho ba đối thủ, phù hợp số quân đã đọc và lá đã thấy.
- Không xem việc đối thủ *có lá để chặn* là chắc chắn họ *sẽ chặn*. Khi chặn một lá 10 bằng J mà đối thủ giả lập đang giữ đôi J, mô hình hạ mức sẵn sàng tách đôi. Phá bộ ba/tứ quý còn được giảm mức sẵn sàng hơn.
- Lịch sử đánh từng đối thủ điều chỉnh ước lượng: hai lượt đánh lẻ cùng số là bằng chứng đã tách đôi; đánh nhiều đôi nguyên bộ gợi ý xu hướng giữ các bộ. Số quân còn lại cũng tác động sự khẩn cấp. **Không suy ra bỏ lượt nếu chưa quan sát được bỏ lượt.**
- Cảnh báo chặt heo sử dụng **cấu trúc bài giả lập + hành vi đã quan sát + số bài còn lại**, chỉ hiện khi đến lượt và bạn đang giữ quân 2. Cảnh báo nhắc rủi ro **nếu đánh quân 2**, không khẳng định đối thủ đang có hàng.
- Hai chế độ **xếp hạng 1–2–3–4** (mặc định) và **ưu tiên về nhất** được dùng khi hai nước có rủi ro ước tính gần bằng nhau.

**Quan trọng:** Các tỷ lệ là **điểm rủi ro từ mô hình quy tắc chưa được hiệu chuẩn**, không phải xác suất thực tế hay kết quả AI tự học. Luật chặt đặc biệt, thứ tự chất và tiền thưởng/phạt của trò chơi này chưa được kiểm chứng đầy đủ nên có thể khuyên sai. Không nên xem kết quả là cam kết thắng hoặc bảo vệ tiền cược.

## Build APK với GitHub Actions

1. Giải nén ZIP vào gốc repository GitHub, giữ `.github/workflows/build-apk.yml`.
2. Mở **Actions → Build Chặt Heo Tracker APK → Run workflow**.
3. Workflow cài JDK 17, Android SDK 35, Gradle 8.9, chạy **8 lớp kiểm thử Java** rồi mới build `app-debug.apk`.
4. Khi workflow báo thành công, tải artifact `ChatHeoTracker-v1.4-auto-behavior-advice-apk`.

## Giới hạn kiểm thử

- Đã chạy kiểm thử Java logic, gồm các ván chia ngẫu nhiên, trường hợp phá đôi/bộ ba/tứ quý, số lá không khớp và cảnh báo heo.
- Chưa biên dịch hoàn chỉnh APK trên Android SDK trong môi trường hiện tại; GitHub Actions phải xác nhận bước biên dịch.
- Chưa kiểm tra độ chính xác của nhận diện lượt và bài tay trên thiết bị Xiaomi 17 Pro Max thực tế ở tất cả tình huống. Chat/icon che, quân nhô lên hoặc trò chơi đổi giao diện có thể khiến app yêu cầu quét lại.
