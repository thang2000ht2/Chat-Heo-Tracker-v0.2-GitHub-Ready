# Chặt Heo Tracker v1.1 – Monte Carlo Strategy (bản thử nghiệm)

## Có gì mới

- Nút **Gợi ý** chạy khi người chơi chủ động bấm.
- Phân rã **toàn bộ tổ hợp hợp lệ trong tay** để tìm số lượt tối thiểu xả bài còn lại (không phải chỉ chọn bộ nhiều lá).
- Khi đã có đủ bài công khai, bài tay hiện tại và số lá của cả ba đối thủ khớp bộ 52 lá: lấy mẫu **192 cách phân chia bài còn ẩn**, ước lượng tần suất từng người có thể chặn một nước đánh.
- Cho 3 lựa chọn theo điểm tương đối; chú ý rủi ro giữ/đánh heo, trả giá nếu bị chặt, đối thủ đang gần về.
- Dữ liệu không đủ, trùng hoặc đọc thiếu: ghi rõ **không mô phỏng / yêu cầu chỉnh dữ liệu**, không bịa xác suất thắng.
- Xử lý chiến thuật ở luồng nền, giữ nguyên HUD nhỏ và bản ghi lịch sử từ trái sang phải.
- Chạy thêm các bài test Java trước build APK trên GitHub Actions.

## Điểm chưa làm

- **Chưa huấn luyện mạng nơ-ron**, chưa đạt kỹ thuật ISMCTS hoàn chỉnh và chưa kiểm thử độ mạnh ở ván thật.
- **Chưa nhận diện toàn bộ bài trên tay** tự động; bạn phải nhập lại **bài đang còn trên tay sau mỗi lượt**.
- Bạn phải cập nhật thủ công **bộ bài đang cần chặn** hoặc xóa nội dung khi mở lượt.
- Luật chặt heo đặc biệt và cách quy đổi tiền/phạt thực tế cần xác minh từ đúng phiên bản game; tính toán hiện là **proxy**, không đại diện tiền thắng thực tế.
- Không dùng ảnh/quyền root/đọc tiến trình game. Mọi ảnh xử lý trong thiết bị.

## Build bằng GitHub Actions

1. Giải nén thư mục gốc, bao gồm `.github/workflows/build-apk.yml`, `app/`, `build.gradle`, `settings.gradle`.
2. Tải lên repository GitHub (Private được). Không đặt trùng thư mục gốc.
3. Tab **Actions** → **Build Chặt Heo Tracker APK** → **Run workflow**.
4. Khi tất cả bước xanh, tải Artifacts **ChatHeoTracker-v1.1-monte-carlo-preview-apk** và giải nén lấy `app-debug.apk`.
5. Nếu lỗi build, gửi **log cụ thể**; không nên tiếp tục dùng APK khi có lỗi nhận diện bài.

Không cần Gradle Wrapper cho GitHub: workflow cài Gradle 8.9 bằng `gradle/actions/setup-gradle`.

## Gợi ý thời điểm test

- Mở Tracker nhập bài **hiện còn**, cấp quyền ghi màn hình và overlay rồi vào ván.
- Chọn đúng bài **cần chặn**, hoặc xóa nếu bạn đang mở lượt.
- Chỉ bấm `Gợi ý` khi muốn xem đề xuất. Nếu thiếu bài công khai hoặc số bài không khớp sẽ báo chưa mô phỏng.
- Đo hiệu suất: độ trễ thực tế trên Xiaomi, số quân đã nhận so với nhìn bằng mắt, các đề xuất có đúng luật Chặt Heo bạn đang chơi hay không.

Bản v1.1 là **mã nguồn để bạn build**, không phải APK đã biên dịch trong môi trường này.
