# Biên dịch Chặt Heo Tracker v1.6 qua GitHub Actions

1. Giải nén `ChatHeoTracker_v1.6_OfflineLearning_GitHub.zip`.
2. Đưa toàn bộ **nội dung** thư mục vào repository GitHub cũ, gồm `.github/workflows/build-apk.yml` (thư mục có dấu chấm đầu). **Thay toàn bộ các file phiên bản cũ**, không chỉ bổ sung Java mới.
3. Mở **Actions → Build Chặt Heo Tracker APK → Run workflow**.
4. Workflow kiểm thử **10 nhóm Java** rồi thực hiện `:app:assembleDebug`. Chỉ khi dòng `Build debug APK` thành công mới lấy artifact `ChatHeoTracker-v1.6-learning-players-apk`, giải nén để có `app-debug.apk`.
5. Nếu gặp lỗi, mở job có dấu X và gửi log từ dòng `error:` đầu tiên trở đi.

**Chưa được biên dịch/kiểm thử trên Xiaomi 17 Pro Max trong môi trường tạo file**. Những mô phỏng chỉ mang tính ước lượng, không chứng minh bài trên tay đối thủ. Không cần token, dịch vụ AI hay quyền Internet.
