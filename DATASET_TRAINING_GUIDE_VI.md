# Chặt Heo Tracker v2.2 – dữ liệu thật để huấn luyện AI

## Điều thay đổi so với v2.1

Bản v2.1 có bộ phân tích Monte Carlo cùng trọng số quy tắc, nhưng **chưa có mô hình học máy được huấn luyện, kiểm chứng trên dữ liệu thực**. Bản 2.2 thêm nền tảng thu thập dữ liệu chính xác và **không đưa mô hình mới chưa kiểm chứng vào bộ gợi ý**.

### Thu thập dữ liệu tự động trong lúc chơi

App ghi JSONL trong bộ nhớ ứng dụng (không dùng Internet). Một dòng là một sự kiện, mỗi sự kiện có `round_id`, `time_ms` và nguồn/độ tin cậy khi cần:

- `hand_verified`: bài trên tay đã xác nhận qua nhiều khung hình; `hand_partial` chỉ là dữ liệu chưa tin cậy.
- `opponent_count`: biến động số bài của ba đối thủ sau khi ổn định ít nhất hai khung hình.
- `opponent_move`: các lá bài xác nhận đã đánh, ghép với người có số bài giảm; `matched_exact` mới được dùng làm nhãn loại nước đánh.
- `unattributed_play` và `missing_play`: quan sát chưa xác định, KHÔNG dùng như dữ liệu nhãn đúng.
- `seat_transition`: thay đổi người ngồi vị trí, không lưu tên, ảnh avatar hay dấu vân tay của nickname trong nhật ký JSONL.
- `advice_proposed`: nước mà thuật toán *đề xuất*, với `result_is_actual_play=false`. Không dùng sự kiện này như một nước đã đánh.
- `round_end`: hệ thống reset/ván mới, kết quả mặc định `unknown`.
- `round_result`: bạn chủ động chọn **Hạng 1/2/3/4** trong ứng dụng sau ván; mới là nhãn hạng được người dùng xác nhận.

Tự động thu thập mặc định được bật theo yêu cầu. Trong màn hình Tracker có công tắc tắt, nút **Xuất JSONL**, **Xóa nhật ký** và bốn nút **Hạng 1–4**. Ứng dụng vẫn có kho hành vi cũ để điều chỉnh quy tắc theo các lượt đã quan sát; chức năng này không phải một mạng nơ-ron mới.

**Lưu ý:** Dữ liệu được giữ theo vòng tối đa khoảng 8 MB trong hai tệp để tránh đầy bộ nhớ. Hãy xuất dữ liệu định kỳ nếu muốn huấn luyện qua rất nhiều ván. Không xóa app hoặc xóa dữ liệu ứng dụng trước khi xuất dữ liệu: thao tác đó có thể làm mất bộ nhật ký trên điện thoại.

### Huấn luyện và kiểm thử bằng máy tính offline

Sau khi xuất `chatheo-observations.jsonl` từ điện thoại, copy vào máy tính, chạy:

```bash
python3 training_tools/train_move_patterns.py chatheo-observations.jsonl --out opponent_move_model.json
```

Chương trình **không cần Internet hay thư viện ngoài**. Nó chỉ chọn những lượt đối thủ đã được xác nhận tương ứng với việc giảm chính xác số quân trên tay. Dữ liệu được chia **theo ID ván** (không chia ngẫu nhiên các nước đánh trong cùng một ván). Chương trình cần ít nhất **150 nước đánh xác nhận từ 30 ván**, nếu không sẽ báo **NOT TRAINED** và đợi thêm dữ liệu. Trên dữ liệu giữ lại, nếu không cải thiện log-loss so với phân phối nền, chương trình sẽ báo **NOT SAVED**.

Mô hình thử nghiệm hiện chỉ ước lượng tần suất đối thủ đánh **bài lẻ, đôi, bộ ba, tứ quý, sảnh hay bộ khác** theo giai đoạn ván. Nó **không suy ra bài úp**, chưa tính xác suất chặt heo chính xác, không đo tỷ lệ thắng và **không được đưa tự động vào app**. Để tạo ra AI chơi tốt hơn, giai đoạn sau phải xây bộ luật được kiểm chứng cho tất cả trường hợp chặt, ghi nhãn xếp hạng và tiền thưởng/phạt, huấn luyện/mô phỏng chính sách mới và thử trên các ván chưa dùng để huấn luyện.

### Chất lượng & công bằng đánh giá

1. Không coi việc không nhìn thấy quân bài là bằng chứng đối thủ không có quân đó.
2. Không ghi lời khuyên AI thành nước đánh thật; ghi nhận nước thật riêng khi có hình ảnh và đối chiếu.
3. Không nối nhầm dữ liệu hai người khác nhau chỉ vì dấu hiệu nickname tương tự.
4. Chia tập huấn luyện/kiểm thử theo ván, sau này còn cần tách theo đối thủ và thời gian để đo khả năng tổng quát hóa.
5. Kết quả hạng 1–4 phải được xác nhận, không suy luận từ việc bảng đếm tắt hoặc một người rời phòng.
6. Cần bạn cung cấp thể lệ chặt chồng, ba/bốn đôi thông, tiền phạt… trước khi mô hình quyết định dựa trên giá trị thưởng/phạt thật.

### Quyền riêng tư

Lưu dữ liệu trong máy; không tự gửi ảnh, tài khoản hay kết quả ra Internet. Nếu bạn tự xuất và chia sẻ JSONL, các quân bài và lịch sử từng ván sẽ có trong tệp. Bạn có thể tắt và xóa nhật ký bất kỳ lúc nào. **Kho hồ sơ hành vi đối thủ của phiên trước và nhật ký huấn luyện là hai vùng dữ liệu khác nhau.**

**Khi chơi có tính điểm/phạt:** Bạn có thể nhập số điểm hoặc số tiền lãi/lỗ ròng thực tế (dấu âm nếu thua) bên cạnh nút chọn hạng. Trường này tùy chọn, không tự suy luận từ bộ bài. Sau khi có đủ nhiều ván có nhãn, mới có thể kiểm thử mô hình dự đoán hạng và điểm ròng. Bản huấn luyện mẫu hiện chưa khai thác trường này.
