# Kiến trúc Steve V2

`forge` phụ thuộc `agent-core`. Core không được import `net.minecraft`, `net.minecraftforge` hay tham chiếu ngược adapter Forge.

## Luồng hoạt động

1. Command server nhận yêu cầu sau khi kiểm tra người sở hữu.
2. Adapter Minecraft thu snapshot trên server thread: tọa độ, dimension, owner, biome, block/entity gần đó và lịch sử.
3. `HttpPlanner` nhận snapshot bất biến và cấu hình được truyền vào, gọi HTTP bất đồng bộ. Không đọc thế giới trong callback.
4. `PlanParser` parse phản hồi và `TaskSchema` kiểm tra toàn bộ kế hoạch. Task không hợp lệ làm kế hoạch bị từ chối; không âm thầm loại bỏ bước phụ thuộc.
5. `AgentRuntime.tick()` nhận kết quả, tạo action thông qua factory và thực thi trên server thread.
6. Kết quả được ghi vào memory và gửi riêng cho owner. Thất bại dừng các tác vụ phía sau; tự replan là bước nâng cấp tiếp theo.

## Vòng đời

Trạng thái: IDLE → PLANNING → EXECUTING → IDLE hoặc FAILED; PAUSED giữ tiến độ và tạm ngừng tick action.

Lệnh mới thay thế nhiệm vụ cũ. `stop()` hủy future, xóa tham chiếu tới kết quả cũ, hủy action và dọn hàng đợi. Cho dù HTTP request không hủy được, kết quả về muộn không thể chạy lại task cũ.

World reload giữ entity và lịch sử, nhưng không tự tiếp tục công việc cũ. Thao tác pause/stop không hoàn tác các block đã đặt/đào.

## Xây nhiều Steve

Build nhóm được lưu theo ServerLevel, owner, loại công trình và vùng anchor gần nhau. WorkQueue phát lease cho từng worker; chỉ commit sau khi đặt block thành công. Worker dừng/unload thì lease trả vào hàng đợi. Một worker có thể lấy hết công việc còn lại.

Đây là phối hợp trên server thread, không phải các Java thread cùng sửa Minecraft world. Worker đang thực hiện cập nhật nhịp hoạt động; nhóm không hoạt động quá 1.200 tick được loại khỏi danh sách tìm kiếm. Build nhóm chưa được lưu qua restart.

## Ranh giới chưa triển khai

Chưa có perception theo vị trí tài nguyên, inventory, AI tự sửa kế hoạch, kiểm tra claim của mod khác hay giới hạn khu vực làm việc. Build có thể thay block tại vùng đã chọn; tính năng preview/undo cần làm trước khi sử dụng để sửa công trình quan trọng.

GUI chỉ gửi command đến server. Không lấy integrated-server instance từ client, không gọi LLM từ client, không chia sẻ API key qua networking.
