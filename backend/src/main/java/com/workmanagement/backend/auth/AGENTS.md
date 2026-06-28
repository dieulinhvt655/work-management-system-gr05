# Auth Module

Module xác thực phiên đăng nhập. Thuộc nhóm **UC 1.0 – Quản lý người dùng và phân quyền**.

## Use Cases

| ID | Use case | Actor | FR liên quan |
|----|----------|-------|--------------|
| UC-1.1 | Đăng nhập | Guest User | FR-1.2 |
| UC-1.2 | Đăng xuất | User đã log in | FR-1.2 |
| UC-1.3 | Khôi phục mật khẩu | Guest User | FR-1.3 |

## Phạm vi module

- Đăng ký / đăng nhập, cấp và làm mới JWT (access + refresh token).
- Đăng xuất (vô hiệu hóa refresh token nếu có).
- Quy trình quên mật khẩu và đặt lại mật khẩu.

## Không thuộc phạm vi

- Quản lý hồ sơ cá nhân → `user/`
- CRUD tài khoản người dùng (admin) → `user/`
- Phân quyền / vai trò → `security/`

## Controllers & DTOs chính

- `AuthController`
- Request: `LoginRequest`, `RegisterRequest`, `RefreshTokenRequest`, `ForgotPasswordRequest`, `ResetPasswordRequest`
- Response: `LoginResponse`, `RegisterResponse`, `TokenResponse`
