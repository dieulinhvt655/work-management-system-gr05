# Dashboard Module

Module hiển thị dashboard tổng quan. Thuộc nhóm **UC 7.0 – Quản lý Dashboard**.

## Use Cases

| ID | Use case | Actor | FR liên quan |
|----|----------|-------|--------------|
| UC-7.1 | Xem Dashboard tổng quan Workspace | Workspace Owner | FR-7.1 |
| UC-7.2 | Xem Dashboard Team | Team Leader | FR-7.1 |
| UC-7.3 | Xem Dashboard dự án | Project Manager | FR-7.1 |
| UC-7.4 | Xem Dashboard cá nhân | Team Member | FR-7.1 |

## Phạm vi module

- Tổng hợp metrics theo phạm vi: workspace, team, project, cá nhân.
- Trả về dữ liệu read-only cho biểu đồ / widget (tiến độ, workload, sprint status…).
- Phân quyền theo actor — mỗi vai trò chỉ xem dashboard phù hợp.

## Liên quan module khác

- Đọc dữ liệu từ: `workspace/`, `team/`, `project/`, `sprint/`, `task/`, `user/`

## Controllers chính

- `DashboardController`
- `DashboardMapper`
