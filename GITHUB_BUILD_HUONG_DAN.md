# Hướng dẫn GitHub Actions — v2.1

1. Giải nén dự án, tải các thư mục và tệp bên trong lên repository GitHub, bao gồm `.github/workflows/build-apk.yml`.
2. Mở **Actions** → **Build Chặt Heo Tracker APK** → **Run workflow**.
3. Workflow phải chạy qua 18 bộ kiểm thử Java trước khi chạy `:app:assembleDebug`.
4. Khi thành công, tải artifact `ChatHeoTracker-v2.1-tempo-redeal-apk`, giải nén và lấy file `app-debug.apk`.
5. Nếu Actions thất bại, chia sẻ lỗi đầu tiên ở bước tương ứng, không chỉ dòng cuối `exit code 1`.

> Cảnh báo: một APK debug biên dịch trên máy GitHub Actions mới có thể dùng chữ ký khác bản bạn đã cài. **Không gỡ app cũ** nếu muốn giữ hồ sơ tự học và chưa có bản sao lưu. Để nâng cấp ổn định cần cấu hình cùng keystore lâu dài; workflow này chưa triển khai ký bằng khóa cố định.

Kiểm thử Java thành công không chứng minh tính chính xác ảnh trên Android 16. Chỉ đánh giá lỗi quét/đếm/reset sau khi cài và thử trên điện thoại thực.

## Cập nhật v2.2

GitHub Actions tự kiểm tra class `TrainingDataStore`, `TestTrainingDataStore`, `training_tools/test_train_move_patterns.py` và biên dịch `app-debug.apk`. Artifact: `ChatHeoTracker-v2.2-training-dataset-apk`.
