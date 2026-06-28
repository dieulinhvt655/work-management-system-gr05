package com.workmanagement.backend.common.constant;

/**
 * Mã lỗi chuẩn mà API trả về cho frontend thông qua {@code ApiResponse.errorCode}.
 * Quy ước prefix: SYS_ (hệ thống), AUTH_ (xác thực), USER_ (người dùng).
 * Thêm mã mới theo từng module khi implement service tương ứng.
 */
public final class ErrorCode {

    private ErrorCode() {
    }

    // --- System ---
    public static final String INTERNAL_ERROR = "SYS_001"; // Lỗi hệ thống không lường trước (500).
    public static final String VALIDATION_ERROR = "SYS_002"; // Request không hợp lệ (@Valid, thiếu/sai field).

    // --- Auth ---
    public static final String INVALID_CREDENTIALS = "AUTH_001";/** Email hoặc mật khẩu không đúng khi đăng nhập. */
    public static final String TOKEN_EXPIRED = "AUTH_002";  /** JWT đã hết hạn. */
    public static final String TOKEN_INVALID = "AUTH_003"; /** JWT không hợp lệ hoặc bị sửa đổi. */
    public static final String UNAUTHORIZED = "AUTH_004";  /** Chưa đăng nhập hoặc không có quyền truy cập. */

    // --- User ---
    public static final String USER_NOT_FOUND = "USER_001";  /** Không tìm thấy user theo id/email/username. */
    public static final String EMAIL_ALREADY_EXISTS = "USER_002";  /** Email đã được đăng ký. */
    public static final String USERNAME_ALREADY_EXISTS = "USER_003"; /** Username đã được sử dụng. */
    public static final String USER_INACTIVE = "USER_004"; /** Tài khoản đã bị vô hiệu hóa (inactive/deleted). */

    // --- Role & Permission ---
    public static final String ROLE_NOT_FOUND = "ROLE_001"; /** Không tìm thấy vai trò theo id. */
    public static final String PERMISSION_NOT_FOUND = "PERM_001"; /** Không tìm thấy quyền theo id. */
    public static final String PERMISSION_ALREADY_EXISTS = "PERM_002"; /** Mã quyền đã tồn tại. */

}
