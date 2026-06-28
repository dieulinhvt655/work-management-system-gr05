# Security Module

Module phân quyền, vai trò và quyền hạn. Thuộc nhóm **UC 1.0 – Quản lý người dùng và phân quyền**.

## Use Cases

| ID | Use case | Actor | FR liên quan |
|----|----------|-------|--------------|
| UC-1.9 | Cập nhật vai trò người dùng | Workspace Owner, Admin | FR-1.5 |

## Phạm vi module

- Quản lý Role và Permission (CRUD, gán quyền cho vai trò).
- Gán / thay đổi vai trò của người dùng trong workspace.
- JWT filter, `SecurityConfig`, `SecurityUtils` — hạ tầng bảo mật dùng chung.

## Liên quan module khác

- Thực thể User → `user/`
- Xác thực đăng nhập → `auth/`

## Controllers & Entities chính

- `RoleController`, `PermissionController`
- Entity: `Role`, `Permission`, `RolePermission`
- Request: `CreateRoleRequest`, `UpdateRoleRequest`, `AssignPermissionsRequest`, `CreatePermissionRequest`, `UpdatePermissionRequest`
- Response: `RoleResponse`, `PermissionResponse`
