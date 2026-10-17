# Báo cáo thử thuật toán Java thật – 17/10/2026

**Mục đích:** thử đúng `MonteCarloAdvisor.adviseBehaviorAware` Java, không dùng bot Python giả làm AI ứng dụng.

- 20 cách chia bài cho mỗi loại đối thủ, bốn vị trí ghế, 5 kiểu bot => tổng 400 trận Java và 400 trận đối chứng. Tất cả dùng **cùng bộ chia và cùng ghế** giữa Java và bot thấp nhất. 6.556 lần gọi Java gợi ý.
- Engine Python có luật tham chiếu, chưa xác nhận là Pig Hunters. Tự chơi 4 người, xếp hạng 1–4; bot không học từ dữ liệu thực.
- Khoảng tin cậy 95% là bootstrap theo **cách chia**, mỗi nhóm 4 ghế được lấy mẫu cùng nhau. Với n=20, CI còn rộng; không thể đánh giá +1% đáng tin.

| Đối thủ | Java về nhất | Chênh so với bot đánh thấp nhất | Khoảng 95% chênh | Lượt dự phòng | Qua gate? |
|---|---:|---:|---:|---:|---|
| Đánh thấp nhất | 23,75% | −1,25 điểm % | [−7,50; +6,25] | 18 | Không |
| Xả nhiều lá | 17,50% | +10,00 điểm % | [0,00; +20,00] | 18 | Không |
| Giữ heo | 17,50% | +10,00 điểm % | [0,00; +18,75] | 18 | Không |
| Ngẫu nhiên hợp lệ | 37,50% | +6,25 điểm % | [−8,75; +21,25] | 18 | Không |
| Bỏ lượt chiến thuật | 27,50% | +13,75 điểm % | [+1,25; +27,50] | 18 | Không |

**Cách đọc:** ví dụ 17,50% là Java về nhất khi gặp 3 bot giữ heo trong engine tham chiếu, không phải tỷ lệ thắng ở Pig Hunters. `fallbacks` = số lần phải thay quyết định Java bằng nước đi dự phòng. Gate đặt trước: ứng viên phải cải thiện ≥1 điểm phần trăm, CI 95% chênh không chứa 0, **không có fallback/vi phạm luật**; vì vậy tất cả đều **không đạt**, kể cả dòng có khoảng tin cậy dương.

## Kết luận kỹ thuật
- App Java chưa chứng minh tốt hơn baseline bằng thống kê có ý nghĩa. Không nên “nâng AI” bằng tăng trọng số tùy ý.
- Một phần lỗi nước đi có thể liên quan khác biệt luật mở đầu 3♠ và luật so đồng hạng giữa Java/Python; phải chốt thể lệ Pig Hunters trước khi kết luận agent sai.
- Cần dùng benchmark 200–1.000 bộ chia và kiểm thử bot chiến thuật hơn. Chạy workflow `benchmark.yml`, tăng thời gian và lưu dữ liệu từng trận.
- Tín hiệu bỏ lượt cần được xác nhận và ghi cùng bộ bài cần chặn; không dùng absence-of-play làm nhãn pass.
- Xác suất chặn phải hiệu chuẩn theo ván thật với Brier/ECE trước khi đưa phần trăm ra HUD.
