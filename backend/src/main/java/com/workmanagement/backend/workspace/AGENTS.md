# Workspace Module

Module quản lý workspace / tổ chức. Thuộc nhóm **UC 2.0 – Quản lý Workspace / tổ chức**.

## Use Cases

| ID | Use case | Actor | FR liên quan |
|----|----------|-------|--------------|
| UC-2.1 | Tạo và thiết lập thông tin ban đầu cho workspace | Workspace Owner, Admin | FR-2.1 |
| UC-2.2 | Cập nhật thông tin Workspace | Workspace Owner, Admin | FR-2.2 |
| UC-2.6 | Cập nhật thông tin tổ chức của thành viên trong Workspace | Workspace Owner, Admin | FR-2.4 |
| UC-2.10 | Đóng / Vô hiệu hoá Workspace | Workspace Owner, Admin | FR-2.8 |

## Phạm vi module

- Tạo workspace, cấu hình thông tin ban đầu.
- Cập nhật metadata workspace (tên, mô tả, trạng thái).
- Quản lý thành viên workspace (`WorkspaceMemberController`): thêm, cập nhật thông tin tổ chức.
- Đóng hoặc vô hiệu hoá workspace.

## Liên quan module khác

- Nhóm làm việc (team) → `team/`
- Theo dõi hoạt động tổ chức (UC-2.9) → `activitylog/`
- Dashboard workspace (UC-7.1) → `dashboard/`

## Controllers & DTOs chính

- `WorkspaceController`, `WorkspaceMemberController`
- Request: `CreateWorkspaceRequest`, `AddWorkspaceMemberRequest`, `UpdateWorkspaceMemberRequest`
