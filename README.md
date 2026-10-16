# Chặt Heo Tracker v0.7 — Auto Reset & Responsive HUD

Phiên bản này kế thừa v0.6 và thực hiện các yêu cầu về giao diện:

- **Reset bài tại bàn:** nút ↻ 42dp ở **góc trên trái**, nằm trong một cửa sổ riêng có thể nhấn; các ô hiển thị dữ liệu vẫn bấm xuyên qua.
- **Tự reset sau khi chia lại:** yêu cầu ván trước đã ghi nhận đối thủ giảm bài, sau đó cả ba ô P1/P2/P3 đều đọc là 13 trong ít nhất 5 khung hình lấy mẫu liên tiếp (~1,5 giây) để tránh reset do chuyển cảnh.
- **Kích thước ô tự điều chỉnh:** rộng theo nội dung thực tế; giới hạn chiều ngang để tự xuống dòng. Bảng Chưa đánh được neo với mép dưới phía trên tay bài. Khi chưa có bài, các ô P1/P2/P3 ẩn; Bạn/khác ẩn nếu trống.
- **Bỏ nhãn thừa:** chỉ còn P1, P2, P3, không viết Trên/Trái/Phải. Bỏ dòng tổng số đã thấy / giải thích màu đỏ; cảnh báo thiếu nhận diện chỉ hiển thị khi có vấn đề.
- **Giữ đủ bài chưa đánh:** gộp chất theo số (vd `7♠♣♦♥`) và tô đỏ nhóm còn đủ bốn chất.
- **Không cản nút Hủy/Đánh:** bảng phía dưới giới hạn trong 57,5% chiều ngang bên trái, không nhận chạm. Reset là ngoại lệ và chỉ chiếm đúng hình vuông 42dp ở góc trái.

## Giới hạn

- Tự reset chỉ hoạt động khi OCR nhận diện đủ ba số 13. Nếu một ô bị che hoặc bị đọc sai, hãy chạm ↻.
- Reset chỉ xóa dữ liệu trong ứng dụng theo dõi, không tác động đến game gốc.
- Đây là mã nguồn thử nghiệm, **chưa biên dịch/kiểm thử trên Xiaomi trong phiên làm việc này**. Độ chính xác nhận diện bài và người chơi vẫn có thể sai.
- `Chưa đánh` có nghĩa là chưa **được bộ nhận diện ghi nhận đã đánh**, không đồng nghĩa chắc chắn vẫn nằm trong tay đối thủ.

## Biên dịch

Xem file `GITHUB_BUILD_HUONG_DAN.md`. Workflow nằm ở `.github/workflows/build-apk.yml`.

---

# Chặt Heo Tracker v0.6 – Compact HUD (thử nghiệm)

**Mục tiêu:** không che hai nút Hủy/Đánh bài hoặc cản thao tác khi chơi.

- Giữ các ô P1/P2/P3 ở gần đối thủ, như v0.5.
- Đưa **Bạn/khác** và **Chưa đánh** vào một HUD ngắn ở phía trái-trung tâm ngay phía trên tay bài (tọa độ game ngang: x ~1,5–59,5% chiều rộng, y ~61% chiều cao); phần bên phải từ 60% tới 100% được chừa trống để thao tác.
- Thay `7♠ 7♣ 7♦ 7♥` thành **`7♠♣♦♥`**: vẫn thể hiện đủ từng lá nhưng chỉ lặp số một lần. Nếu đủ 4 chất chưa được ghi nhận đã đánh thì cả nhóm hiện **màu đỏ**.
- Hiển thị toàn bộ các lá chưa đánh bằng cách tự xuống dòng; không vuốt ngang và không cắt sau 22 lá.
- Tất cả overlay dùng `FLAG_NOT_TOUCHABLE` và `LayoutParams.alpha=0.70`, nên không có vùng chặn nhấn trên lớp phủ; đồng thời vẫn tránh đặt overlay trên vùng nút thao tác.
- Nút **Đặt lại khi bắt đầu ván mới** ở trong màn hình chính của Tracker; không còn nút có thể bấm trên HUD.
- Giữ chức năng đọc số lá và gán bài cho P1 (trên), P2 (trái), P3 (phải) của v0.4/v0.5.

## Build trên GitHub

1. Giải nén ZIP, đưa các tệp và thư mục vào gốc repo. Giữ nguyên `.github/workflows/build-apk.yml`.
2. Mở tab Actions > **Build Chặt Heo Tracker APK** > **Run workflow**.
3. Sau khi thành công, tải artifact `ChatHeoTracker-v0.6-compact-hud-apk` lấy `app-debug.apk` để cài.
4. Đây là bản thử nghiệm. Chưa biên dịch APK hoặc thử chơi trực tiếp trên Xiaomi 17 Pro Max trong môi trường này.

## Lưu ý

- `Chưa đánh` nghĩa là những lá **chưa được ghi nhận đã đánh**: có thể vẫn nằm trên tay bạn hoặc đối thủ. Nếu nhận diện bỏ sót, dữ liệu còn sai.
- Màu đỏ chỉ biểu thị chưa thấy lá nào của cùng số, không kết luận đối thủ có tứ quý.
- Tọa độ tối ưu theo video/ảnh giao diện nằm ngang mà bạn gửi; nếu nút Đánh/Hủy ở vị trí khác trên một chế độ game, có thể cần điều chỉnh tiếp.

---

# Chặt Heo Tracker v0.5 — bố cục đầy đủ lá (bản thử nghiệm)

## Thay đổi từ v0.4

- Xóa HorizontalScrollView và SingleLine: các lá được xuống dòng tự động, **không bị cắt sau 22 lá và không cần cuộn ngang**.
- Tách thành 4 cửa sổ nổi: P1 gần người trên, P2 gần người trái, P3 gần người phải, và bảng cuối (Bạn/khác + Chưa đánh) phía trên bài của bạn.
- Bảng cuối hiển thị tất cả **52 lá ban đầu**, trừ các quân app đã ghi nhận được là đã đánh; sắp xếp 3 đến 2, trong hạng theo ♠ ♣ ♦ ♥.
- Các quân **còn đủ bốn chất cùng hạng chưa ghi nhận là đã đánh** được tô **đỏ tươi** ở dòng Chưa đánh. Đây không phải bằng chứng ai đó đang cầm tứ quý: 4 lá có thể phân tán ở các tay và có thể có lá trên tay bạn.
- Tự điều chỉnh kích thước phông và vị trí khi xoay sang màn hình ngang.
- Giữ nguyên xác định người đánh bằng biến động số bài của P1 / P2 / P3 từ v0.4; không cần chọn lượt thủ công.
- `FLAG_SECURE` trên các cửa sổ giúp hạn chế chữ trong overlay bị quét lại như bài trong hình ghi màn hình. Vì thế cửa sổ nổi có thể **không xuất hiện trong bản ghi màn hình**, dù vẫn nhìn thấy trên điện thoại.

## Giới hạn cần biết

- Ứng dụng vẫn là **mã nguồn chưa biên dịch/kiểm thử trên Xiaomi**; độ chính xác nhận diện quân và ghép người chơi chưa được chứng nhận. Nếu mất các lá trong lúc quét, ô Chưa đánh không thể tự suy ra lá bị bỏ sót.
- Cửa sổ có thể đè lên vài thành phần của game tùy độ phân giải / cỡ chữ HyperOS. Cần thử trên máy thật và tinh chỉnh tọa độ nếu cần.
- Độ dài các danh sách không bị giới hạn trong mã nguồn UI. Vị trí bảng cuối có thể chiếm một phần phần trên của tay bài nếu chế độ hiển thị chữ trên máy quá lớn.
- Việc tô đỏ chỉ phụ thuộc các lá **chưa từng được ghi nhận là đã đánh**, không loại riêng lá đang nằm trên tay bạn.
- Sử dụng công cụ hỗ trợ trò chơi có thể trái điều khoản game.

## Build APK qua GitHub Actions

1. Giải nén file ZIP và đưa **toàn bộ nội dung**, gồm cả `.github/workflows/build-apk.yml`, lên GitHub.
2. Mở **Actions → Build Chặt Heo Tracker APK → Run workflow**.
3. Khi job hoàn tất, tải artifact `ChatHeoTracker-v0.5-full-cards-apk`, bên trong có `app-debug.apk`.
4. Cài trên Xiaomi Android 16, mở app và cấp quyền **hiển thị trên ứng dụng khác** cùng **ghi toàn bộ màn hình**.

Đây là ZIP **mã nguồn**; môi trường chuẩn bị chưa có Android SDK để build / kiểm thử APK.

---

# Chặt Heo Tracker v0.4 — Tự nhận biết người vừa đánh

## Cách hoạt động

- P1 = người ngồi trên; P2 = người ngồi bên trái; P3 = người ngồi bên phải.
- Khi số bài bên cạnh avatar giảm (ví dụ 13 -> 10), ứng dụng ghép thay đổi đó với 3 lá bài vừa xuất hiện giữa bàn.
- Không bắt buộc chọn người đi đầu hoặc chuyển lượt P1/P2/P3 thủ công.
- Sau khi người chơi ra hết bài, thẻ đếm có thể biến mất; bản này có thêm phát hiện về 0 khi số trước đó <= 3.
- Hàng cuối cùng `Chưa đánh` = 52 trừ số lá được nhận diện là đã đánh. Nó bao gồm cả các lá còn trong tay chính bạn.
- `Bạn/khác` ghi các lá đã xuất hiện mà không đối chiếu được với sự giảm số bài của một trong 3 đối thủ.
- Các hàng sắp xếp rank tăng dần 3,4,5,6,7,8,9,10,J,Q,K,A,2; trong cùng rank là Bích, Chuồn, Rô, Cơ.
- Các hàng có thể vuốt ngang để xem đủ số lá, không còn cắt sau 22 lá.

## Hạn chế

- Đây là bản thử nghiệm, không thể đảm bảo nhận đủ 52 lá hoặc gán chính xác 100% trên mọi thiết bị.
- Template số 6 và 8 là ảnh mẫu gần đúng vì video tham chiếu không chứa hai số đó trong ô đếm.
- Đầu ván cần thấy ít nhất 2 khung hình số bài của từng đối thủ để đặt mốc. Nếu bắt đầu ghi màn hình quá muộn, có thể không biết mốc ban đầu.
- Nếu hai lượt diễn ra quá nhanh hoặc bài bị che, hiển thị `Bạn/khác` hoặc cảnh báo bỏ sót thay vì gán bừa.
- Vẫn có nút `Ván mới` dự phòng; ván mới cũng được phát hiện tự động khi ba đối thủ cùng trở về 13 sau khi đã giảm.
- Thuật toán đã được đối chiếu trên các khung hình tham chiếu của người dùng nhưng APK chưa được kiểm thử trên điện thoại thực tế trong môi trường này.

## Build APK bằng GitHub Actions

1. Giải nén ZIP và tải **nội dung** vào thư mục gốc repository, gồm `.github/workflows/build-apk.yml`, `app`, `build.gradle`, v.v.
2. Mở tab Actions > Build Chặt Heo Tracker APK > Run workflow.
3. Sau khi chạy thành công, tải Artifact `ChatHeoTracker-v0.4-auto-player-apk` chứa `app-debug.apk`.
4. Cài thử trên Xiaomi Android 16 và cấp quyền hiển thị cửa sổ nổi, ghi màn hình (Toàn bộ màn hình).

**Lưu ý:** Đây là mã nguồn, chưa phải APK đã biên dịch. Chưa xác nhận GitHub Actions biên dịch thành công cho phiên bản này.

---

# Chặt Heo Tracker v0.2 — mã nguồn thử nghiệm Android 16

**Chưa phải APK đã biên dịch hoặc đã chạy thử trên điện thoại.** Đây là dự án Android Studio Java. Chưa kiểm thử trên Xiaomi 17 Pro Max chạy Android 16. Bản cũ 0.1 chỉ nhận lớp bài trắng; bản này thử cả các lớp bài màu xám và trắng ở giữa bàn.

## Đầu vào đã dùng để hiệu chỉnh

- Hai ảnh chụp màn hình ván khác (2048 × 942): bàn trống và bộ ba `3♠ 4♥ 5♦`.
- Video game Chặt Heo (116 giây, khung hình gốc 2608 × 1200), dùng để tìm tọa độ quét, nhận biết 2 lớp bài chồng nhau, và tạo mẫu chữ 8, K, 10.
- Có 13 mẫu chữ quân từ `3` đến `2`, và 4 chất bài. Không đính kèm video/ảnh người dùng trong ZIP.

## Kết quả kiểm thử và giới hạn

- Khi chạy **thuật toán Python tham khảo**, 18/18 khung hình đã chọn, đối chiếu thủ công và dùng để hiệu chỉnh nhận đúng các lá bài nhìn thấy. Đây **không** phải phép đo độ chính xác độc lập, không bảo đảm chạy đúng trên từng frame khác hoặc trên thiết bị thật.
- Hai ảnh ban đầu: `[ ]` (chưa đánh), `[3♠, 4♥, 5♦]`.
- Khi mô phỏng trên cả video bằng cách lấy 1 hình/giây và chỉ ghi nhận khi 2 mẫu liên tiếp trùng nhau, số quân phân biệt ghi nhận được là 39. Đây **không** có nghĩa game chỉ có 39 lá đã ra: khung hình lấy mẫu có thể bỏ qua các lượt rất nhanh, còn thiếu sự xác nhận bằng thiết bị thật.
- Video cho thấy quân bài có thể xuất hiện trên lớp **xám** (lượt cũ) và lớp **trắng** (lượt mới). Bộ quét mới xử lý cả hai lớp và hạn chế nhầm hình chất lớn ở cạnh phải thành một quân bài thứ hai.
- Chưa có kiểm thử độ trễ/hiệu năng hoặc so khớp 52 lá suốt một ván trực tiếp. Các kết quả đọc sai/còn thiếu có thể thêm/gỡ thủ công trong app.
- Ứng dụng **chưa tự nhận diện bài trên tay bạn** và **chưa biết chính xác lá bài úp của đối thủ**. Chỉ số `52 - đã đánh` không có nghĩa chỉ còn bấy nhiêu lá trên tay đối thủ.
- Chưa tự động phát hiện ván mới, dùng nút "Ván mới" khi bắt đầu.
- Cần quyền chiếu màn hình và hiển thị cửa sổ nổi, không bảo đảm game cho phép chia sẻ màn hình. Sử dụng công cụ hỗ trợ trò chơi có thể trái điều khoản của game.

## Mã nguồn

- `app/src/main/java/vn/chatheotracker/MainActivity.java`: giao diện xin quyền và bổ sung/gỡ bài thủ công.
- `app/src/main/java/vn/chatheotracker/CaptureService.java`: foreground service Android `mediaProjection`, ảnh chụp bằng ImageReader, quét trung bình ~300ms, cửa sổ nổi; đòi nhận giống nhau ở hai lần liên tiếp trước khi ghi.
- `app/src/main/java/vn/chatheotracker/CardRecognizer.java`: template glyph và phân vùng **gray/white** đã chuyển từ mô hình thử Python. Đếm mỗi mã lá một lần.
- `app/src/main/assets`: mẫu quân và chất.
- `video_detector.py`, `test_video_frames.py`, `replay_video.py`: chương trình kiểm thử bằng Python/NumPy/OpenCV. Cần đưa đường dẫn tới **video riêng** khi chạy; bản ZIP không chứa video.
- `train_from_video.py`: tái tạo mẫu 8, K, 10 từ đúng video và đúng tọa độ cố định; chỉ dùng cho phát triển.
- `test_on_screenshots.py`, `tools_train.py`: mã kiểm thử/huấn luyện từ bản v0.1, không phải thuật toán Python v0.2 chính.
- `replay_results.txt`: kết quả mô phỏng 1 hình/giây, không phải kết quả chạy trên Xiaomi.

## Cách biên dịch thành APK

1. Cài Android Studio cùng SDK (compileSdk 35) và JDK 17.
2. Mở thư mục `ChatHeoTracker_Source`, đồng bộ Gradle (cần có kết nối để tải plugin/phụ thuộc nếu chưa có).
3. **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
4. Sau khi build thành công, APK debug mặc định ở `app/build/outputs/apk/debug/app-debug.apk`.

Chưa có Android SDK/build tools trong môi trường chuẩn bị bộ mã nguồn, vì vậy không có file APK để cài ngay và không thể bảo đảm dự án build thành công trên các phiên bản Android Studio cụ thể. ZIP cũng không có Gradle wrapper.

## Chạy trên Xiaomi 17 Pro Max / Android 16

Cấp quyền hiển thị trên ứng dụng khác, nhấn bắt đầu và cấp quyền chia sẻ **toàn bộ màn hình**, quay về game. Có thể kéo cửa sổ nổi và bấm "Ván mới". Khi nhận sai, mở lại Tracker để dùng hai nút thêm/gỡ quân. Hệ thống Android có thể yêu cầu cấp quyền ghi hình lại mỗi lần bắt đầu; thử điều chỉnh quyền chạy nền/cửa sổ nổi theo cấu hình HyperOS nếu bị dừng.

**Quyền riêng tư:** Mã nguồn không khai báo quyền Internet; ảnh được xử lý trên điện thoại, không ghi lại thành video hoặc tự gửi lên server.


## Bản v0.3 (players)
- Bỏ giới hạn chỉ hiển thị 22 lá.
- Popup tách riêng P1 / P2 / P3 và dòng Chưa đánh.
- Các lá trên mỗi dòng được sắp xếp từ bé đến lớn theo thứ tự 3..2, phụ theo chất Bích < Chuồn < Rô < Cơ.
- Thêm nút Đổi lượt trên popup và Chọn lượt tiếp theo trong màn hình chính để sửa vòng gán người chơi.
