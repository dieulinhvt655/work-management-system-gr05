# Workspace Module

Module quản lý workspace / tổ chức. Thuộc nhóm **UC 2.0 – Quản lý Workspace / tổ chức**.

## Use Cases

| ID | Use case | Actor | FR liên quan |
|----|----------|-------|--------------|
| UC-2.1 | Tạo và thiết lập thông tin ban đầu cho workspace | System Admin | FR-2.1 |
| UC-2.2 | Cập nhật thông tin Workspace | Workspace Owner, Admin | FR-2.2 |
| UC-2.6 | Cập nhật thông tin tổ chức của thành viên trong Workspace | Workspace Owner, Admin | FR-2.4 |
| UC-2.10 | Đóng / Vô hiệu hoá Workspace | Workspace Owner, Admin | FR-2.8 |

## Phạm vi module

- Tạo workspace, cấu hình thông tin ban đầu.
- Danh sách toàn bộ Workspace chỉ dành cho System Admin.
- System Admin tra cứu chi tiết Workspace theo id; owner và member ACTIVE xem Workspace của mình qua `/current`.
- Chỉ System Admin hoặc owner được chỉ định trên Workspace được cập nhật thông tin; member chỉ có quyền xem.
- Cập nhật Workspace chỉ thay đổi tên/mô tả; `close` xử lý Active → Inactive và `reactivate` dành cho System Admin xử lý Inactive → Active.
- Cấu hình mặc định khi tạo: trạng thái Active, owner là thành viên có role Workspace Owner;
  các role Workspace Owner, Team Leader, Project Manager và Team Member phải tồn tại.
- Mỗi user chỉ được thuộc một Workspace; `workspace_members.user_id` là duy nhất.
- Role Workspace Owner được đồng bộ vào `User.role`; API member không được gán thêm Workspace Owner.
- Owner/member không được truy cập Workspace Inactive; lịch sử tổ chức yêu cầu `workspace:activity-read`.
- Cập nhật metadata workspace chỉ gồm tên và mô tả.
- Quản lý thành viên workspace (`WorkspaceMemberController`): thêm, cập nhật thông tin tổ chức.
- Đóng hoặc vô hiệu hoá workspace.

## Liên quan module khác

- Nhóm làm việc (team) → `team/`
- Theo dõi hoạt động tổ chức (UC-2.9) → `activitylog/`
- Dashboard workspace (UC-7.1) → `dashboard/`

## Controllers & DTOs chính

- `WorkspaceController`, `WorkspaceMemberController`
- Request: `CreateWorkspaceRequest`, `AddWorkspaceMemberRequest`, `UpdateWorkspaceMemberRequest`
