# Tracker v2.4 – Kiểm chứng thuật toán Java và phân tích bộ hàng chưa thấy

## Đã thực sự thêm
- `AdvisorBenchmarkBridge.java`: sử dụng **chính** `MonteCarloAdvisor.adviseBehaviorAware` của ứng dụng trong engine tự đấu Python; không thay bằng một heuristic Python đại diện.
- `evaluation/selfplay_java_advisor.py`: đối đầu với 5 loại bot: thấp nhất, xả nhiều, giữ heo, ngẫu nhiên hợp lệ và bỏ lượt chiến thuật. Cùng bộ chia, bốn vị trí cho Java và baseline. Nước không hợp lệ/không có gợi ý được **đếm riêng và chuyển sang baseline dự phòng**, không được âm thầm tính như nước AI.
- `evaluation/test_java_benchmark.py`: ngưỡng phát hành +1 điểm phần trăm và CI 95% không chứa 0, không có lỗi nước đi.
- `.github/workflows/benchmark.yml`: workflow chạy benchmark với số bộ chia tự chọn; `build-apk.yml` chạy smoke test 2 bộ chia.
- `ThreatScenarioSampler.java`: 512 cách chia bài còn phù hợp các số đếm; cho từng đối thủ ước lượng tỷ lệ kịch bản **có** 3 đôi thông, 4 đôi thông, tứ quý. Hiển thị tròn theo bậc 5% và gắn `mẫu` để tránh giả dạng xác suất xác thực.
- HUD có nhãn dựa trên các mẫu giả lập gần từng ghế khi đủ dữ liệu; khi bài chưa đối soát đủ, không hiển thị con số thiếu cơ sở. Chạy tính riêng trên luồng nền.
- Trên trang Tracker có các nút xác nhận đối thủ **bỏ lượt** để ghi sự kiện `opponent_pass` với `lead`, `seat`, bộ đếm và `confidence=user_confirmed`. Không kết luận từ việc không thấy đối thủ đánh và chưa tự coi pass là bằng chứng xác suất do chưa có dữ liệu hiệu chuẩn.
- `evaluation/calibration_check.py`: Brier score, ECE và reliability buckets từ dữ liệu **đã có nhãn thật**, từ chối báo cáo khi ít hơn 200 quan sát/30 ván. Không tự nhận mô hình đã hiệu chuẩn.
- `HandRecognizer.java`: chỉ khi quạt không tìm được ở vị trí bình thường mới quét thêm các hàng khác; vẫn phải nhận đúng template từng quân.
- Trạng thái quét thiếu bài ghi `góc x/13`, không nhầm số góc nhận diện được với số lá chắc chắn trên tay.

## Chưa làm – quan trọng
- Chưa có bằng chứng mô hình chiến thắng baseline; tuyệt đối **không** gắn nhãn ISMCTS hay ML đã huấn luyện.
- Không có detector visual-confirmed pass đủ tin cậy. Hiện chỉ ghi pass được người dùng xác nhận (qua app chính, không che HUD).
- Luật chặt chồng / tới trắng / ngoài lượt / tiền phạt vẫn chưa được chứng thực từ **Pig Hunters**.
- Các tỷ lệ 3ĐT/4ĐT/TQ là tần suất trong kịch bản giả lập, **không** phải xác suất đối thủ chắc chắn có hàng, cũng **không** phải xác suất họ sẽ chặt.
- Chưa có tập ảnh nhiều thiết bị có nhãn chính xác để đo precision/recall từng quân. Bài bị che hoàn toàn không thể được suy ra chắc chắn.
- Chưa biên dịch Android APK trực tiếp trên môi trường này, cần kiểm thử GitHub Actions/Xiaomi.

## Phép thử đã thực hiện
- 20 bộ bài độc lập cho mỗi đối thủ, xoay bốn ghế, có đối chứng ghép cặp; 6.556 lần gọi tới Java advisor.
- Không chiến thuật nào đạt ngưỡng phát hành đặt trước vì còn các lần fallback do engine tham chiếu không chấp nhận nước hoặc không có đề xuất.
- Xem `evaluation/results/java_advisor_20/paired_summary.json`, `paired_trials.csv` để kiểm tra từng ván và CI bootstrap theo cách chia bài.
- Bài kiểm thử Pure Java và các kiểm thử Python hiện có đã đạt.

## Cách sử dụng
Cập nhật toàn bộ mã nguồn GitHub, chạy **Build Chặt Heo Tracker APK**. Chọn **Benchmark actual Java advisor (research only)** → Run workflow → số bộ chia 20/100/300. Report tải ở Artifacts. Không áp dụng tự động thuật toán candidate nếu benchmark chưa qua gate.
