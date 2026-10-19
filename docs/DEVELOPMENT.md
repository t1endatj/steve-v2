# Phát triển Steve V2

## Bản đầu

Nền tảng: Minecraft 1.21.1 / Forge 52.1.0 / Java 21, Gradle wrapper 8.12.1.

### Thành phần hiện có

- Entity và renderer Steve; đăng ký entity/attributes theo Forge 1.21.1.
- Command server và màn hình K tối giản, dùng chung quyền điều khiển trên server.
- Task, action interface, planner và memory model trong core Java thuần.
- HTTP planner cho ba provider với API key/model tách riêng.
- Di chuyển, đi theo, đào, đặt block, chiến đấu; bộ sinh nhà/lâu đài/tháp/tường/nền/chuồng/nhà hiện đại/hộp.
- Nạp template qua Minecraft structure manager, giữ block state.
- WorkQueue và coordinator phân phối công việc xây cho các Steve.

### Xử lý tác vụ

- Chuẩn hóa tham số `block`, `quantity`, `player`; schema kiểm tra kiểu, giới hạn, tọa độ trước thực thi.
- Dừng và thay nhiệm vụ loại bỏ future cũ, xóa queue cũ.
- Xóa goal khi hoàn thành; quay lại hành vi theo owner khi rảnh.
- Không tự xóa/spawn lại Steve khi người chơi đăng nhập.
- Hủy navigation/flying sau action và khi entity unload.
- Fallback procedural được gọi thực sự khi không có template.
- Build không tính phần việc đang claim là đã đặt thành công; trả việc khi worker dừng.
- Build nhóm phân biệt level và owner; không nhập mọi công trình cùng tên trên toàn server.
- Combat dùng cooldown thay vì gây sát thương mỗi tick.
- Snapshot và action chạy trên game thread; HTTP chỉ dùng snapshot.

### Xác minh

- `gradlew build`: kiểm tra biên dịch hai module, unit tests và đóng JAR.
- `gradlew :forge:runGameTestServer -PgameTests`: kiểm tra trong server Minecraft không cần tài khoản/API key.
- Client rendering, thao tác K và gọi provider thật cần chạy thử bằng Minecraft client/API key của người dùng.

Không coi alpha này là bản survival hoàn chỉnh. Các giai đoạn tiếp theo: UI progress/preview → inventory và công cụ → crafting/smelting → phục hồi/replan có giới hạn → lưu/khôi phục nhiệm vụ → thêm loader khi cần.

## Alpha.2: sửa status và follow sau phản hồi chơi thử

- Bỏ tên Bob cố định trong UI; chọn từ các entity client đang thấy, nhớ tên khi đóng màn hình, hỗ trợ server chọn target khi lệnh không có tên.
- Command feedback status dùng system message riêng để không phụ thuộc sendCommandFeedback; thêm gợi ý tên và danh sách tên khi không tìm thấy.
- Follow theo UUID người chơi khi dùng command; bỏ timeout 5 phút cho lệnh follow liên tục.
- Ground navigation được mở/đi qua cửa gỗ; explicit và idle follow cùng dùng FollowController.
- Theo dõi tiến triển, phục hồi tới vị trí có nền và không va chạm trong assistantMode, báo rõ trạng thái trong status.
- Thêm kiểm tra người chơi bay, follow lặp lại, follow sau xây, phòng kín, cửa đóng, status và chọn tên GUI.
