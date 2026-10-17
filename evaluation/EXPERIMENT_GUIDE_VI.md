# Kế hoạch chứng minh AI có ích hơn các chiến thuật cơ bản

1. **Xác minh luật Pig Hunters trước:** ba bích mở đầu, chặt đôi heo/chặt chồng, bốn đôi thông ngoài lượt, tới trắng, luật xếp hạng và bảng phạt. Bộ engine kèm theo chỉ là bộ luật tham chiếu (xem RULE_COVERAGE.md).
2. **Đánh giá trên bộ bài công bằng:** cùng seed/cùng cách chia cho cả bốn thuật toán; xoay vị trí ghế để giảm thiên lệch; mỗi cách chia là một đơn vị thống kê. Công cụ: `python3 evaluation/selfplay.py --games 1200 --seed 314159 --output evaluation/results`.
3. **Chấm hạng thực tế:** đo xác suất hạng 1/2/3/4, hạng trung bình, tiền lãi/lỗ nếu có thể lệ đã kiểm chứng. Bổ sung khoảng tin cậy bootstrap theo **cách chia bài**, không lấy 4 ghế cùng cách chia là bốn thử nghiệm độc lập.
4. **Lượt bỏ:** không suy từ việc màn hình không thấy quân bài. Khi có nguồn quan sát chắc chắn, ghi `opponent_pass`, `lead`, `seat`, `confidence`, `round_id`, số quân còn; thông tin này chỉ điều chỉnh xác suất bài úp, không loại cứng khả năng đối thủ vẫn có lá chặn.
5. **Dữ liệu ván thật:** xuất JSONL qua Tracker, ghi kết quả thật. Kiểm tra bằng `python3 evaluation/evaluate_results.py file.jsonl`. Kết quả chưa đủ để suy ra tác động gợi ý vì chưa biết người chơi có thực sự làm theo. Cần log nước **đã thực sự đánh**, quyết định có theo gợi ý, và phân nhóm thử nghiệm công bằng trước khi gọi A/B test.
6. **Hiệu chuẩn xác suất:** chỉ khi có nhãn cơ hội chặn và chọn chặn với bằng chứng quan sát. Đánh giá Brier/logloss, ECE và reliability diagram trên tập giữ lại theo ván; đừng in tỷ lệ phần trăm cho rủi ro dựa trên hằng số thủ công.
7. **ISMCTS:** triển khai sau khi engine luật và detector đã chính xác, có thể dùng stratified sampling để giảm phương án bài úp phi lý. Chỉ triển khai lên điện thoại khi đo độ trễ và lợi ích so với baseline.

Lưu ý: các con số kết quả trong `evaluation/results/selfplay_summary.json` là của chính engine mô phỏng đơn giản, không dự đoán chất lượng app ngoài đời.

**Thử nghiệm Bayesian pass:** `evaluation/pass_evidence.py` từ chối suy luận nếu chưa có dữ liệu hiệu chuẩn; khi có, dùng tỉ số likelihood mà không loại hẳn tay bài có khả năng chặn. Không liên kết Android ở phiên bản này.
