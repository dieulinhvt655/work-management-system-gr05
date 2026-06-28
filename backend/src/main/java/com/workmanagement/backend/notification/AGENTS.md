# Notification Module

Module thông báo hệ thống. Không có use case riêng trong danh sách FR, nhưng hỗ trợ nhiều UC nghiệp vụ.

## Vai trò

- Gửi thông báo khi có sự kiện: gán task, phê duyệt công việc, bình luận mới, thay đổi sprint…
- Hỗ trợ gián tiếp các UC cộng tác và vận hành (UC 5.x, UC 6.x).

## UC liên quan (gián tiếp)

| Nhóm | Use cases có thể kích hoạt thông báo |
|------|--------------------------------------|
| UC 5.0 | UC-5.4, UC-5.5, UC-5.7 — phân công, phê duyệt công việc |
| UC 6.0 | UC-6.1 — bình luận mới |
| UC 2.0 | UC-2.8 — phân bổ thành viên dự án |

## Controllers chính

- `NotificationController`
