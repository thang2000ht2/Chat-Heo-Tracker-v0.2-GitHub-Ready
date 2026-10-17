# Chặt Heo Tracker v2.7 — Bait evidence · Advice return-turn recovery

Source project Android 16 + GitHub Actions. **Build APK:** `.github/workflows/build-apk.yml` → Actions → Build Chặt Heo Tracker APK → Run workflow. Artifact: `ChatHeoTracker-v2.7-bait-reentry-training-apk`.

**Thay đổi so với v2.6:** tự gợi ý lại khi bàn được dọn sau một vòng bỏ lượt và nút Đánh đang hoạt động; tính lại gợi ý khi dữ liệu quét lỗi đã hồi phục; tách hàng đợi gợi ý khỏi tính xác suất bộ của đối thủ; thêm nhật ký quan sát áp lực bài cao có thể là chiêu nhử heo. Khi thấy nhiều dấu hiệu, hiển thị nhãn cảnh giác nhẹ dưới avatar, **không xác nhận đối thủ chắc chắn có hàng**. Tùy chọn tự lưu dataset vẫn có, nhưng app không tự train mô hình từ vài ván.

**Giữ các sửa lỗi từ v2.6:** bài tay và bài đã đánh được loại trừ khỏi nhóm hàng đối thủ có thể cầm; tô màu theo từng lá/chất, không tô nhầm quân của bạn; tự cập nhật 3ĐT, 4ĐT, tứ quý từ các tình huống bài úp giả lập khi dữ liệu cho phép. Các % này vẫn là **% mẫu**, không phải xác suất được hiệu chuẩn.

Chi tiết: `CHANGELOG_V2_7.md`, `TRAINING_BAIT_GUIDE_VI.md`, `CHANGELOG_V2_6.md`, `GITHUB_BUILD_HUONG_DAN.md`.

**Cảnh báo kiểm chứng:** Java/Python tests chạy offline không chứng minh tính năng hoạt động trên HyperOS/Xiaomi; chưa có luật Pig Hunters chuẩn hoặc mô hình dự đoán hành vi dụ heo được huấn luyện từ các chuỗi thắng/thua xác nhận.
