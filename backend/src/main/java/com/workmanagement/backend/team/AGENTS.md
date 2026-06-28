# Team Module

Module quản lý nhóm làm việc trong workspace. Thuộc nhóm **UC 2.0 – Quản lý Workspace / tổ chức**.

## Use Cases

| ID | Use case | Actor | FR liên quan |
|----|----------|-------|--------------|
| UC-2.3 | Tạo nhóm làm việc trong Workspace | Workspace Owner, Admin | FR-2.3 |
| UC-2.4 | Cập nhật thông tin nhóm làm việc trong Workspace | Workspace Owner, Admin | FR-2.3 |
| UC-2.5 | Giải thể nhóm | Workspace Owner, Admin | FR-2.3 |
| UC-2.7 | Gán Team Leader cho từng nhóm làm việc | Workspace Owner, Admin | FR-2.5 |
| UC-2.8 | Phân bổ thành viên vào dự án và gán vai trò trong dự án | Team Leader | FR-2.6 |

## Phạm vi module

- CRUD nhóm làm việc (team) trong workspace.
- Quản lý thành viên nhóm (`TeamMemberController`).
- Gán / thay đổi Team Leader.
- Hỗ trợ Team Leader phân bổ thành viên nhóm sang dự án (phối hợp với `project/`).

## Liên quan module khác

- Workspace chứa team → `workspace/`
- Gán thành viên dự án chi tiết → `project/` (`ProjectMemberController`)
- Dashboard team (UC-7.2) → `dashboard/`

## Controllers & DTOs chính

- `TeamController`, `TeamMemberController`
- Request: `AddTeamMemberRequest`
