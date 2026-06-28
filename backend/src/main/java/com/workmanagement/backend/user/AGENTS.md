# User Module

Module quản lý tài khoản và hồ sơ người dùng. Thuộc nhóm **UC 1.0 – Quản lý người dùng và phân quyền**.

## Use Cases

| ID | Use case | Actor | FR liên quan |
|----|----------|-------|--------------|
| UC-1.4 | Quản lý hồ sơ cá nhân | User đã log in | FR-1.4 |
| UC-1.5 | Tạo tài khoản người dùng | Workspace Owner, Admin | FR-1.1 |
| UC-1.6 | Xem danh sách tài khoản người dùng | Workspace Owner, Admin | FR-1.1 |
| UC-1.7 | Cập nhật thông tin tài khoản người dùng | Workspace Owner, Admin | FR-1.1 |
| UC-1.8 | Khoá / Mở khoá tài khoản người dùng | Workspace Owner, Admin | FR-1.1 |

## Phạm vi module

- Người dùng tự cập nhật profile (`UpdateProfileRequest`).
- Admin / Workspace Owner tạo, liệt kê, cập nhật trạng thái tài khoản.
- Khoá và mở khoá tài khoản (`UpdateUserStatusRequest`).

## Liên quan module khác

- Gán vai trò người dùng (UC-1.9) → `security/` (`UpdateUserRoleRequest`, `UserRoleResponse`).
- Đăng nhập / đăng xuất → `auth/`.

## Controllers & DTOs chính

- `UserController`
- Request: `UpdateProfileRequest`, `UpdateUserStatusRequest`
- Response: `UserRoleResponse` (khi trả về thông tin vai trò)
