# Steve V2

Minecraft **1.21.1**, **Forge 52.1.0**, **Java 21**. Mod ID: `steve_v2`.

Dự án có core Java độc lập và adapter Minecraft Forge. Fabric sẽ được xem xét sau.

## Nguồn core và phần kế thừa

Steve V2 phát triển từ ý tưởng core và một phần code của [Steve AI — YuvDwi/Steve](https://github.com/YuvDwi/Steve), tham chiếu commit `a9475973332be040bb5095d6edf4cd3dc69c344e`. Các action và thuật toán đào, di chuyển, đặt block, chiến đấu, nhận biết môi trường và sinh công trình được kế thừa rồi chỉnh sửa. Runtime, task schema, HTTP planner và hàng đợi xây được tổ chức lại trong `agent-core`, tách khỏi adapter Forge. Xem [ghi nhận nguồn](docs/ATTRIBUTION.md).

## Các thay đổi và lỗi đã sửa

- Nâng nền tảng lên Minecraft 1.21.1, Forge 52.1.0 và Java 21.
- Dừng hoặc thay nhiệm vụ sẽ dọn action/hàng đợi và bỏ kết quả AI về muộn; hoàn thành thì xóa goal. Có pause/resume cả khi đang chờ planner.
- Kiểm tra toàn bộ kế hoạch trước thực thi: tên tham số, kiểu dữ liệu, tọa độ, số lượng và kích thước công trình.
- Sửa GUI hỏi sai tên Steve: tự chọn entity đang thấy, nhớ tên khi đóng bằng Esc; lệnh có thể tự chọn khi chỉ có một Steve được phép điều khiển.
- Status phản hồi qua system message, không phụ thuộc `sendCommandFeedback`; hiển thị tọa độ, khoảng cách và trạng thái di chuyển.
- Follow dùng UUID người chơi khi gọi qua command, mở cửa gỗ, xử lý bị kẹt/đi xa trong assistantMode và theo người chơi bay từ mặt đất. Dùng chung xử lý cho follow chủ động và khi rảnh.
- Dọn navigation/trạng thái bay sau action; bổ sung kiểm tra follow lặp lại và follow sau khi xây.
- Xây nhóm dùng hàng đợi chung, chỉ tính hoàn thành sau khi đặt block thành công và trả việc khi worker dừng; nhóm tách theo level và owner.
- Dùng bộ sinh công trình khi không có template; nạp template `.nbt` qua Minecraft structure manager và giữ block states.
- Combat có cooldown; entity, owner và lịch sử được lưu qua NBT, không tự xóa/spawn lại khi đăng nhập.

Đã ghi nhận **18 unit tests và 10 Minecraft GameTests pass** cho alpha.2; phạm vi kiểm tra và phần chưa xác minh nằm trong [báo cáo kiểm thử](docs/VERIFICATION.md). Import `.schem`/`.litematic`, preview/undo và survival inventory chưa được triển khai.

## Chạy dự án

Cài JDK 21, trỏ `JAVA_HOME` tới JDK đó, rồi chạy tại thư mục này:

```powershell
.\gradlew.bat build
.\gradlew.bat :forge:runClient
```

JAR cài vào thư mục `mods` của Minecraft Forge 1.21.1:

```text
forge/build/libs/steve-v2-forge-2.0.0-alpha.2.jar
```

Core được đóng gói sẵn trong JAR mod. Không cần cài JAR `agent-core` riêng.

## Dùng thử không cần API key

```text
/steve spawn Bob
/steve follow Bob
/steve build Bob house
/steve status Bob
/steve pause Bob
/steve resume Bob
/steve stop Bob
/steve remove Bob
/steve list
```

Nhấn **K** để mở bảng nhập tên Steve và giao nhiệm vụ. Phản hồi và tiến độ được gửi riêng cho chủ sở hữu qua chat. Mỗi Steve thuộc về người tạo; chủ sở hữu hoặc operator được điều khiển. Các lệnh tìm Steve trong các chunk đã tải ở dimension hiện tại.

Từ alpha.2, giao diện tự chọn tên Steve đang hiện diện thay vì điền sẵn `Bob`. Tên nhập được nhớ cả khi đóng bằng Esc. Nếu chỉ có một Steve bạn được điều khiển trong dimension, có thể dùng `/steve status`, `/steve follow`, `/steve pause`, `/steve resume` hoặc `/steve stop` không cần tên. Nếu có nhiều Steve, chỉ định tên hoặc dùng Tab để chọn.

Follow mở được cửa gỗ và tiếp tục cho tới khi nhận lệnh khác hoặc người chơi rời dimension. Status có tọa độ, khoảng cách và mô tả đang đi/chờ/bị kẹt. Trong assistantMode, nếu bị kẹt khoảng 3 giây hoặc xa hơn 24 block, Steve có thể dịch chuyển tới mặt đất an toàn gần người chơi. Khi người chơi bay Creative, Steve theo ở mặt đất bên dưới; không dịch chuyển vào khoảng không. Idle follow dùng chung cơ chế phục hồi này. Không tự tải chunk để đuổi theo người chơi đã dịch chuyển quá xa.

## Giao việc bằng ngôn ngữ tự nhiên

Sau lần khởi chạy đầu, sửa `config/steve_v2-common.toml` ở phía server (với `runClient`: `forge/run/config/steve_v2-common.toml`). Chọn provider và điền API key cùng model đang khả dụng của đúng provider. Xem `config/steve_v2-common.toml.example`.

```text
/steve tell Bob xây một ngôi nhà bằng gỗ
/steve tell Bob đào 20 quặng sắt
/steve tell Bob đi theo tôi
```

Hỗ trợ OpenAI/Groq qua chat completions và Gemini qua generateContent. Model/API key để trống có chủ ý: chọn model tương thích với endpoint và tài khoản của bạn. Hiện chưa kiểm chứng cuộc gọi AI thật bằng API key. Những model có yêu cầu tham số riêng có thể cần adapter bổ sung.

## Cấu trúc

```text
agent-core/   Task schema, parser, planner HTTP, agent runtime, memory, work queue, unit tests
forge/        Entity, world queries, actions, command, GUI, config, NBT, GameTests
docs/         Kiến trúc, tiến độ phát triển, kết quả kiểm thử
```

Core không phụ thuộc Minecraft hoặc Forge. Chỉ snapshot dạng dữ liệu được chuyển cho planner; server thread giữ toàn bộ quyền truy cập thế giới.

## Kiểm thử

```powershell
.\gradlew.bat :agent-core:test
.\gradlew.bat :forge:runGameTestServer -PgameTests
```

GameTests chạy trong world kiểm thử riêng và không cần API key. Chúng kiểm tra spawn, lưu/đọc NBT, đặt block, đào, xây với hai Steve và xây nhà hoàn chỉnh bằng một Steve. Source/template của GameTests chỉ được đưa vào khi có `-PgameTests`; build phát hành dùng `build` thông thường.

## Phạm vi bản alpha

- Đã có: spawn/remove, task có kiểm tra dữ liệu, lập kế hoạch bất đồng bộ, dừng/tạm dừng/tiếp tục, status, đi theo, pathfind, mine, place, attack, procedural build và chia việc xây.
- Entity, chủ sở hữu và tối đa 20 dòng lịch sử được lưu theo NBT. Tác vụ đang chạy dừng khi unload/restart; chưa khôi phục tiến độ làm việc tự động.
- Có thể nạp template từ data pack qua `data/steve_v2/structure/<name>.nbt`. Giữ block states; chưa sao chép block entity NBT như đồ trong rương hoặc chữ trên biển.
- Mặc định là **creative assistant**: bay/dịch chuyển khi làm việc, tạo vật liệu/công cụ và miễn sát thương. Chưa phải survival hoàn chỉnh. Tắt `assistantMode` sẽ chặn các action đào/xây chưa có kiểm tra inventory.
- Chưa có: inventory/crafting/smelting, tự lập kế hoạch lại, retry/fallback/cache HTTP, lịch sử hội thoại đầy đủ, blueprint/undo, resume công trình sau restart, Fabric.
- Không chạy JavaScript do AI tạo.

Xem `docs/DEVELOPMENT.md` trước khi tiếp tục mở rộng.
