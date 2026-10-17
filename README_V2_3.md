# Chặt Heo Tracker v2.3 – Evidence & Baselines

Đây là bản nâng cấp trên v2.2, ưu tiên đo lường thay vì gán phần trăm chưa kiểm chứng. Các phần nhận diện màn hình và HUD cũ vẫn giữ nguyên; mô hình live Android vẫn dựa trên heuristic + Monte Carlo chưa hiệu chuẩn.

## Build GitHub
Giải nén toàn bộ, giữ nguyên `.github/workflows/build-apk.yml`, cập nhật repository trên GitHub. Vào Actions → Build Chặt Heo Tracker APK → Run workflow. Artifact dự kiến `ChatHeoTracker-v2.3-evidence-baselines-apk`. Chưa xác nhận build trên GitHub. Để giữ kho học đối thủ tránh xóa dữ liệu khi thay chữ ký, nên xuất JSONL từ v2.2 trước khi gỡ app cũ.

## Đánh giá thuật toán trên máy tính
```sh
python3 evaluation/test_rules.py
python3 evaluation/test_dataset.py
python3 evaluation/selfplay.py --games 500 --seed 314159 --output evaluation/results_new
python3 evaluation/evaluate_results.py observations.jsonl
```

Xem `CHANGELOG_V2_3.md`, `evaluation/RULE_COVERAGE.md` và `evaluation/EXPERIMENT_GUIDE_VI.md` trước khi diễn giải kết quả. **Không lấy kết quả tự đấu trong luật tham chiếu làm xác suất thắng ở Pig Hunters.**

## Tiếp theo cần bổ sung để thành AI thực sự
1. Ảnh trang luật chính thức từ Pig Hunters để xác nhận chặt chồng, tứ quý/đôi thông, mở đầu, tới trắng và tiền phạt.
2. Detector xác nhận lượt bỏ qua nhiều khung hình; lưu sự kiện kèm đối tượng, lượt bài cần chặn và độ tin cậy. Lượt bỏ chỉ là chứng cứ suy giảm xác suất có bài, không phải 'không thể có'.
3. Nhật ký đủ 100–300 ván có hạng người chơi xác nhận, đề xuất gợi ý và nước thực sự đánh, để xây tập đánh giá theo từng ván (không trộn train/test).
4. Chính sách baseline cạnh tranh và ISMCTS, xếp hạng 1–4, phạt thực tế, so sánh có khoảng tin cậy bootstrap, chỉ cập nhật mô hình Android nếu vượt mốc giữ lại.
