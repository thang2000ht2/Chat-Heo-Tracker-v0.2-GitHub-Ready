# Biên dịch Chặt Heo Tracker v0.4 bằng GitHub Actions

Bộ mã nguồn đã có sẵn `.github/workflows/build-apk.yml`; không cần cài Android Studio, Gradle hay Android SDK trên máy của bạn.

## Cách làm trên trình duyệt

1. Tạo tài khoản (nếu chưa có) tại https://github.com.
2. Vào https://github.com/new, tạo một repository mới (nên chọn **Private** nếu chỉ dùng cá nhân). Không cần tick README hay .gitignore khi tạo.
3. Giải nén ZIP này trên máy tính. Vào thư mục `ChatHeoTracker_Source` và tải **toàn bộ nội dung bên trong thư mục** lên repository. Quan trọng: `app`, `build.gradle`, `settings.gradle` và `.github` phải ở ngay thư mục gốc của repo, **không nằm dưới thêm một thư mục `ChatHeoTracker_Source`**.
4. Trong repository, mở **Actions**, nếu GitHub hỏi thì cho phép workflows. Chọn **Build Chặt Heo Tracker APK** → **Run workflow** (chọn nhánh main). Quy trình cũng chạy khi push lên main/master.
5. Chờ tới khi thấy dấu tích xanh. Mở run vừa hoàn thành, cuộn xuống **Artifacts** → tải `ChatHeoTracker-v0.4-debug-apk`.
6. Giải nén artifact để lấy `app-debug.apk`, chuyển sang Xiaomi và mở cài đặt.

## Nếu build báo lỗi

- Kiểm tra lại cấu trúc repo: `.github/workflows/build-apk.yml`, `app/build.gradle`, `settings.gradle` ở root.
- Trong Actions chọn lần build có dấu X đỏ, mở bước lỗi và sao chép log gửi lại ChatGPT để sửa.
- Chỉ APK build thành công mới xuất hiện trong Artifacts.

## Thông tin kỹ thuật

- Android Gradle Plugin: 8.7.3
- Gradle: 8.9 (cài trực tiếp trong runner qua `gradle/actions/setup-gradle@v4`; không yêu cầu gradlew/wrapper)
- Java: 17
- Android compileSdk: 35
- Artifact xuất ra: `app/build/outputs/apk/debug/app-debug.apk`

**Tình trạng:** Đây là mã nguồn thử nghiệm; chưa xác nhận build/hoạt động trên Xiaomi 17 Pro Max vì chưa chạy được CI trên GitHub hay thiết bị. Độ chính xác nhận diện bài chưa kiểm chứng độc lập. Trước khi sử dụng hãy chú ý quy định của game về phần mềm hỗ trợ.
