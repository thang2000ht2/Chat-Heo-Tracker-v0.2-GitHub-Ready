# Chặt Heo Tracker v0.2 — mã nguồn thử nghiệm Android 16

**Chưa phải APK đã biên dịch hoặc đã chạy thử trên điện thoại.** Đây là dự án Android Studio Java. Chưa kiểm thử trên Xiaomi 17 Pro Max chạy Android 16. Bản cũ 0.1 chỉ nhận lớp bài trắng; bản này thử cả các lớp bài màu xám và trắng ở giữa bàn.

## Đầu vào đã dùng để hiệu chỉnh

- Hai ảnh chụp màn hình ván khác (2048 × 942): bàn trống và bộ ba `3♠ 4♥ 5♦`.
- Video game Chặt Heo (116 giây, khung hình gốc 2608 × 1200), dùng để tìm tọa độ quét, nhận biết 2 lớp bài chồng nhau, và tạo mẫu chữ 8, K, 10.
- Có 13 mẫu chữ quân từ `3` đến `2`, và 4 chất bài. Không đính kèm video/ảnh người dùng trong ZIP.

## Kết quả kiểm thử và giới hạn

- Khi chạy **thuật toán Python tham khảo**, 18/18 khung hình đã chọn, đối chiếu thủ công và dùng để hiệu chỉnh nhận đúng các lá bài nhìn thấy. Đây **không** phải phép đo độ chính xác độc lập, không bảo đảm chạy đúng trên từng frame khác hoặc trên thiết bị thật.
- Hai ảnh ban đầu: `[ ]` (chưa đánh), `[3♠, 4♥, 5♦]`.
- Khi mô phỏng trên cả video bằng cách lấy 1 hình/giây và chỉ ghi nhận khi 2 mẫu liên tiếp trùng nhau, số quân phân biệt ghi nhận được là 39. Đây **không** có nghĩa game chỉ có 39 lá đã ra: khung hình lấy mẫu có thể bỏ qua các lượt rất nhanh, còn thiếu sự xác nhận bằng thiết bị thật.
- Video cho thấy quân bài có thể xuất hiện trên lớp **xám** (lượt cũ) và lớp **trắng** (lượt mới). Bộ quét mới xử lý cả hai lớp và hạn chế nhầm hình chất lớn ở cạnh phải thành một quân bài thứ hai.
- Chưa có kiểm thử độ trễ/hiệu năng hoặc so khớp 52 lá suốt một ván trực tiếp. Các kết quả đọc sai/còn thiếu có thể thêm/gỡ thủ công trong app.
- Ứng dụng **chưa tự nhận diện bài trên tay bạn** và **chưa biết chính xác lá bài úp của đối thủ**. Chỉ số `52 - đã đánh` không có nghĩa chỉ còn bấy nhiêu lá trên tay đối thủ.
- Chưa tự động phát hiện ván mới, dùng nút "Ván mới" khi bắt đầu.
- Cần quyền chiếu màn hình và hiển thị cửa sổ nổi, không bảo đảm game cho phép chia sẻ màn hình. Sử dụng công cụ hỗ trợ trò chơi có thể trái điều khoản của game.

## Mã nguồn

- `app/src/main/java/vn/chatheotracker/MainActivity.java`: giao diện xin quyền và bổ sung/gỡ bài thủ công.
- `app/src/main/java/vn/chatheotracker/CaptureService.java`: foreground service Android `mediaProjection`, ảnh chụp bằng ImageReader, quét trung bình ~300ms, cửa sổ nổi; đòi nhận giống nhau ở hai lần liên tiếp trước khi ghi.
- `app/src/main/java/vn/chatheotracker/CardRecognizer.java`: template glyph và phân vùng **gray/white** đã chuyển từ mô hình thử Python. Đếm mỗi mã lá một lần.
- `app/src/main/assets`: mẫu quân và chất.
- `video_detector.py`, `test_video_frames.py`, `replay_video.py`: chương trình kiểm thử bằng Python/NumPy/OpenCV. Cần đưa đường dẫn tới **video riêng** khi chạy; bản ZIP không chứa video.
- `train_from_video.py`: tái tạo mẫu 8, K, 10 từ đúng video và đúng tọa độ cố định; chỉ dùng cho phát triển.
- `test_on_screenshots.py`, `tools_train.py`: mã kiểm thử/huấn luyện từ bản v0.1, không phải thuật toán Python v0.2 chính.
- `replay_results.txt`: kết quả mô phỏng 1 hình/giây, không phải kết quả chạy trên Xiaomi.

## Cách biên dịch thành APK

1. Cài Android Studio cùng SDK (compileSdk 35) và JDK 17.
2. Mở thư mục `ChatHeoTracker_Source`, đồng bộ Gradle (cần có kết nối để tải plugin/phụ thuộc nếu chưa có).
3. **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
4. Sau khi build thành công, APK debug mặc định ở `app/build/outputs/apk/debug/app-debug.apk`.

Chưa có Android SDK/build tools trong môi trường chuẩn bị bộ mã nguồn, vì vậy không có file APK để cài ngay và không thể bảo đảm dự án build thành công trên các phiên bản Android Studio cụ thể. ZIP cũng không có Gradle wrapper.

## Chạy trên Xiaomi 17 Pro Max / Android 16

Cấp quyền hiển thị trên ứng dụng khác, nhấn bắt đầu và cấp quyền chia sẻ **toàn bộ màn hình**, quay về game. Có thể kéo cửa sổ nổi và bấm "Ván mới". Khi nhận sai, mở lại Tracker để dùng hai nút thêm/gỡ quân. Hệ thống Android có thể yêu cầu cấp quyền ghi hình lại mỗi lần bắt đầu; thử điều chỉnh quyền chạy nền/cửa sổ nổi theo cấu hình HyperOS nếu bị dừng.

**Quyền riêng tư:** Mã nguồn không khai báo quyền Internet; ảnh được xử lý trên điện thoại, không ghi lại thành video hoặc tự gửi lên server.


## Bản v0.3 (players)
- Bỏ giới hạn chỉ hiển thị 22 lá.
- Popup tách riêng P1 / P2 / P3 và dòng Chưa đánh.
- Các lá trên mỗi dòng được sắp xếp từ bé đến lớn theo thứ tự 3..2, phụ theo chất Bích < Chuồn < Rô < Cơ.
- Thêm nút Đổi lượt trên popup và Chọn lượt tiếp theo trong màn hình chính để sửa vòng gán người chơi.
