# Project Module

Module quản lý dự án. Thuộc nhóm **UC 3.0 – Quản lý Dự án**.

## Use Cases

| ID | Use case | Actor | FR liên quan |
|----|----------|-------|--------------|
| UC-3.1 | Tạo dự án mới | Team Leader | FR-3.1 |
| UC-3.2 | Xem chi tiết dự án | All actors | FR-3.1 |
| UC-3.3 | Cập nhật thông tin dự án | Team Leader, Project Manager | FR-3.1 |
| UC-3.4 | Quản lý thành viên dự án | Team Leader | FR-3.2 |
| UC-3.5 | Kích hoạt dự án | Team Leader | FR-3.1 |
| UC-3.6 | Xem danh sách thành viên dự án | All actors | FR-3.2 |
| UC-3.7 | Xem danh sách dự án | All actors | FR-3.1 |
| UC-3.11 | Kết thúc dự án | Project Manager | FR-3.5 |
| UC-3.12 | Lưu trữ dự án | Team Leader | FR-3.5 |

## Phạm vi module

- Vòng đời dự án: tạo, cập nhật, kích hoạt, kết thúc, lưu trữ.
- Quản lý thành viên và vai trò trong dự án (`ProjectMemberController`).
- Liệt kê và xem chi tiết dự án.

## Liên quan module khác

- Tài liệu dự án (UC-3.8, UC-3.9) → `attachment/`
- Lịch sử hoạt động dự án (UC-3.10) → `activitylog/`
- Phân bổ thành viên từ team (UC-2.8) → `team/`
- Dashboard dự án (UC-7.3) → `dashboard/`

## Controllers & DTOs chính

- `ProjectController`, `ProjectMemberController`
- Request: `UpdateProjectRequest`
- Response: `ProjectResponse`
