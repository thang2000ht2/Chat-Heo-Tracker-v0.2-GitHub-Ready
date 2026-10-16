# Chặt Heo Tracker v0.7 — build trên GitHub Actions

1. Giải nén ZIP v0.7, tải toàn bộ nội dung giải nén vào **gốc repository GitHub**. Giữ thư mục ẩn `.github` và file `.github/workflows/build-apk.yml`.
2. Vào **Actions → Build Chặt Heo Tracker APK → Run workflow** (hoặc chờ workflow chạy sau khi push lên `main`/`master`).
3. Nếu build thành công, tải artifact `ChatHeoTracker-v0.7-auto-reset-responsive-hud-apk` rồi giải nén để lấy `app-debug.apk`.
4. Cài hoặc nâng cấp APK trên Android 16, cấp quyền hiển thị nổi và ghi **toàn bộ màn hình**.

## Khi sử dụng

- Nút nhỏ **↻** ở **góc trên trái** reset bài thủ công khi đang trong game; chỉ phần nút đó nhận chạm.
- Tự reset: sau khi ván trước có số bài của ít nhất một đối thủ giảm, nếu cả ba đối thủ được đọc là **13 lá** liên tục trong **5 khung lấy mẫu** (khoảng 1,5 giây), app xóa dữ liệu ván cũ rồi tiếp tục ván mới.
- Các ô P1 / P2 / P3 ẩn khi chưa thấy ai đánh; không thêm chữ Trên/Trái/Phải. Bảng **Bạn/khác** ẩn khi trống; bảng **Chưa đánh** luôn hiện và thu chiều ngang theo dữ liệu.
- Bảng thông tin khác không nhận chạm; vùng nút Hủy/Đánh bên phải vẫn được để trống.

**Lưu ý:** Đây là mã nguồn, chưa phải APK. Chưa chạy biên dịch hoặc thử thực tế trên Xiaomi. Bộ đếm 13 lá và tốc độ nhận diện vẫn có thể sai khi game có hiệu ứng hoặc màn hình bị che. Nếu workflow báo lỗi, lấy log của bước lỗi để kiểm tra.
