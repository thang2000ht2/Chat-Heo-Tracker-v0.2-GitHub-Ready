# v2.9 – Verified Hand, Atomic Opponent Moves, Diagnostics

- `HandSyncPolicy`: lần đầu có thể nhận tay 1–12 lá nếu ván đã tiến triển, cần 3 khung ổn định; ảnh 8 lá không còn bị khóa chỉ vì thiếu quét 13 đầu ván.
- `CaptureService.updateVisibleHand`: ưu tiên bộ tay đã đọc vững hơn lịch sử sai; ghi `hand_deck_conflict` và chặn màu/phần trăm có thể gây hiểu nhầm khi tổng lá sai.
- `remainingCards`: đảm bảo lá đang cầm được đưa vào "Chưa đánh" kể cả khi ledger cũ từng ghi nhầm là ra; `OpponentCandidatePool` luôn tô mờ đúng lá tay, kể cả khi lịch sử chưa khả thi.
- `PlayBatchMatcher`: gộp các mảnh quan sát theo bộ đếm người đánh, chỉ ghi `opponent_move` khi đủ số lá; không nhận sớm 3/5 của sảnh và không đưa mảnh rời vào dữ liệu tự học dưới tên đối thủ.
- `LearnedOpponentHistory.retractCurrentRound`: khi bài tay xác nhận xung đột với lần ghi sai, rút mẫu hành vi nghi vấn chỉ trong ván hiện tại, không xóa dữ liệu các ván trước.
- `DiagnosticLog`: JSONL chạy offline riêng biệt, bộ nhớ giới hạn, không có nickname/ảnh; menu Export/Xóa riêng.
- Test thực tế dựa trên ba ảnh ngày 18/10/2026 + test ghép sảnh, giữ riêng log và training, bảo toàn dữ liệu.

**Không tuyên bố đạt độ chính xác tuyệt đối hoặc APK đã được thử trực tiếp trên máy.**
