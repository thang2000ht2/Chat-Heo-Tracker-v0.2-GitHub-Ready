# Cách Tracker v1.6 tự học đối thủ – không dùng API

**Dữ liệu học:** thứ tự các bộ bài đã được xác nhận thuộc về từng ghế trên/trái/phải. Mỗi lượt được lưu như một danh sách 1–13 lá; ranh giới ván được ghi tách riêng. Chỉ ghi sau khi nhận diện quân đã ra và ghép với biến động số quân của đúng đối thủ.

**Bài học được rút ra:** tần suất đánh lẻ, đôi, bộ ba, tứ quý, sảnh; xu hướng dùng quân lớn; số lần đã quan sát cùng một giá trị bài được đánh thành hai lượt lẻ trong **cùng một ván**. Những thống kê này điều chỉnh có giới hạn trọng số lấy mẫu tay bài úp, và xu hướng đối thủ phá bộ để chặn quân lẻ. Ít dữ liệu => dự đoán gần như trung lập; nhiều dữ liệu => dần nghiêng theo lịch sử, không bao giờ khẳng định biết bài trên tay người khác.

**Thay đối thủ:** vùng tên trên chính màn hình (không phải avatar) được chuyển thành mẫu nhị phân ánh sáng, lưu cục bộ cùng lịch sử đã học. Đòi hỏi mẫu đổi ổn định qua nhiều khung hình trước khi kết luận. Khi đối thủ rời chỗ hoặc tên thay đổi, **chỉ hồ sơ học của ghế đó** được xóa; trò chơi đang diễn ra vẫn theo dõi bộ 52 lá toàn cục cho đến lần chia ván kế tiếp. Không tự suy ra từ một khung hình đơn lẻ; không bảo đảm phát hiện mọi thay đổi tên gần giống.

**Mô phỏng:** Monte Carlo sử dụng 192 thế giới có phân bổ các quân chưa biết đúng số bài từng người; cân nhắc lịch sử khi chọn phân bổ. Với từng nước đánh hợp lệ, ước lượng xác suất *có thể và muốn* chặn của mỗi ghế, rồi tổng hợp mức rủi ro. Đây là **heuristic chưa hiệu chuẩn**, không phải xác suất thống kê từ dữ liệu thực chiến được chứng thực.

**Riêng tư:** không tải ảnh, tên tài khoản hoặc lịch sử lên máy chủ; các tín hiệu được lưu trong `SharedPreferences` của app trên Android. Có thể xóa ngay trong ứng dụng bằng nút **Xóa dữ liệu tự học đối thủ**. `android:allowBackup="false"`, không khai báo quyền Internet.

**Kiểm thử hữu ích tiếp theo:** ghi log riêng tỷ lệ cảnh báo so với lượt chặn thật sự (nếu nhận biết đủ lượt chặn/bỏ lượt), tách tập kiểm thử theo ván, so sánh sai số so với phiên bản không học. Không dùng mô phỏng từ chính một ván để vừa học vừa tự tuyên bố đã đạt độ chính xác.
