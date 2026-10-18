# Hướng dẫn thu dữ liệu đo bộ đếm và ghép lượt (v3.4)

Bản v3.4 **chưa đạt chứng cứ 99% đúng**, cần bộ ảnh/video nhiều ván được gắn nhãn bằng mắt, độc lập với kết quả Tracker.

## Ảnh bộ đếm
- Mỗi số **6, 8, 3, 9, 5** tại các vị trí **Trên/Trái/Phải**, ưu tiên ít nhất **20 khung hình từ nhiều ván** cho mỗi cặp số×vị trí; bổ sung số 0 / huy hiệu mất, số 10–13, chat che, emote che, người xem.
- Ảnh gốc có giờ chụp gần thời điểm log. Lưu riêng nhãn thủ công (`top=6`, `left=8`, `right=3`). **Không dùng con số Tracker hiển thị làm nhãn**.
- Ảnh giống nhau trong một hoạt ảnh không phải quan sát độc lập. Tập đánh giá cố định phải tách khỏi ảnh dùng thay mẫu 6/8.

## Video có nhãn nước đánh
- Ghi lại video 1–3 ván hoàn chỉnh trước, bao gồm các nước 3–12 lá, bài xám/bài trắng, đối thủ bỏ lượt, khi một người về, khi chia ván mới.
- Nhãn cho mỗi nước: thời điểm, người đánh, đủ quân thực tế, số quân trước/sau, tình trạng có bị che, ai bỏ lượt, độ rõ của nút Đánh.
- Xuất log JSONL ngay khi gặp lỗi, trước khi Reset để còn đầy đủ sự kiện.
- Các file chỉ giữ trong máy bạn hoặc chia sẻ khi bạn muốn. Xóa/che nickname, số tiền trước khi chia sẻ video; log chẩn đoán không tự lưu ảnh.

## Phát lại chuỗi ảnh
Trong dự án có `test_android_stubs/ReplayScannerFrames.java`, nhận một CSV với hai cột: `elapsed_ms,filename`.
Ví dụ:
```
elapsed_ms,filename
1000,frame001.png
1210,frame002.png
1420,frame003.png
```
Dùng Java 17 và bộ `test_android_stubs` để biên dịch như workflow GitHub. Kết quả CSV ghi bài tay, bài trắng, bộ đếm OCR, bộ đếm đã kiểm tra và lượt ghép. Smoke test mặc định từ **các ảnh riêng biệt**, không được coi là video liên tục.

## Đánh giá JSONL
Lệnh:
```
python3 evaluation/scan_metrics.py chat_heo_scan_diagnostics.jsonl --out scan_report.json
```
Chỉ số theo ghế/số quân đây là *sự kiện được app ghi*, **không phải độ chính xác thực tế**. Nếu có tệp nhãn `ground_truth.jsonl`, dùng thêm `--labels ground_truth.jsonl` để đo đúng/thiếu/sai. Cấu trúc nhãn nằm ở đầu mã `evaluation/scan_metrics.py`.

## Tiêu chí nghiệm thu trước khi huấn luyện chiến thuật
- Mỗi giá trị/cặp vị trí đủ mẫu kiểm định độc lập, không đọc nhầm trên tập đó; tự từ chối khi không rõ.
- Ghi đủ đúng người/đúng quân theo các nhóm 1, 2, 3, 4, 5, 6–8, 9–12 lá; báo cả tỷ lệ bỏ sót và gán sai.
- Chạy lại CI và APK trên Xiaomi; chỉ sau đó mới chạy benchmark Java lớn theo luật game chính xác.
