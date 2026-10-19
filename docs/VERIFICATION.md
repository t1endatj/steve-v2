# Xác minh bản alpha — 2026-10-18

Môi trường: Windows, Oracle JDK 21.0.7, Gradle 8.12.1, Minecraft 1.21.1, Forge 52.1.0.

## Unit tests alpha.2: 18 bài pass

- AgentRuntimeTest: 5 bài — dừng lúc đang chờ AI, thay lệnh xóa queue cũ, kết thúc xóa goal, lỗi action dừng bước phụ thuộc, pause/resume lúc đang planning.
- PlanParserTest: 6 bài — giữ đúng quantity, từ chối tên tham số cũ, từ chối action chưa hỗ trợ, chặn số lượng âm/lẻ/quá lớn, thiếu tọa độ, JSON bọc Markdown.
- WorkQueueTest: 3 bài — một worker làm hết queue, claim chưa tính là hoàn thành, trả việc từ worker bị dừng.
- AgentSelectionTest: 4 bài — chọn datmini thay cho Bob mặc định, nhớ tên đã chọn, không tự chọn khi nhiều tên, giữ tên khi Steve ngoài tầm render client.

## Minecraft GameTests alpha.2: 10 bài pass

Lệnh: `gradlew :forge:runGameTestServer -PgameTests`.

1. Spawn, tick, lưu/tải entity: giữ tên, owner UUID và lịch sử.
2. Đặt block qua AgentRuntime: kiểm tra block thực tế và trạng thái IDLE sau khi xong.
3. Đào quặng qua AgentRuntime: kiểm tra ore biến mất và nhiệm vụ hoàn thành.
4. Hai Steve cùng xây nền 8×8: kiểm tra đủ 64 block và cả hai worker hoàn thành.
5. Một Steve xây nhà không có template: kiểm tra hoàn thành, sàn, hai nửa cửa đúng trạng thái và nhận lệnh follow sau khi xây xong.
6. Follow trên nền phẳng: kiểm tra tọa độ thay đổi và khoảng cách tới người chơi, không chỉ trạng thái EXECUTING.
7. Đi qua cửa gỗ đang đóng rồi nhận lại follow để đi ngược lại; không được dùng teleport để vượt bài kiểm tra.
8. Thoát phòng kín bằng recovery trong assistantMode, không phá tường. Kiểm tra cả explicit follow và idle follow.
9. `/steve status` và `/steve follow` tự chọn Steve của owner; status có tên trả đủ hai lần ngay cả khi ordinary command success feedback tắt.
10. Theo người chơi bay Creative theo phương ngang, giữ Steve ở mặt đất an toàn.

Các test chạy trong server Minecraft thực, không sử dụng API key hay model AI.

Trích log lần chạy alpha.2: `docs/test-results/alpha.2-gametest.txt`.

Trước bản sửa follow, test nền phẳng của alpha.1 cũng pass. Lỗi người dùng báo không được coi là mọi trường hợp follow đều hỏng: log xác nhận GUI hỏi tên Bob trong khi entity tên datmini, và code thiếu xử lý cửa/path bị kẹt. Các test bổ sung kiểm tra riêng những đường đi và lựa chọn này.

## Chưa kiểm tra

- Hiển thị renderer/màn hình K và nhập liệu trên client có giao diện.
- Gọi OpenAI/Groq/Gemini bằng model/API key thật.
- Tương thích modpack, claim protection, cấu trúc data pack có block entity NBT.
- Hiệu năng với số lượng Steve lớn, world nhiều dimension và người chơi đồng thời.

Build phát hành đóng gói core vào JAR Forge; test fixtures không được đóng gói vào JAR.
