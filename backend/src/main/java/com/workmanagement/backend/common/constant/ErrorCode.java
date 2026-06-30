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
    public static final String REFRESH_TOKEN_INVALID = "AUTH_005"; /** Refresh token không hợp lệ hoặc đã bị thu hồi. */

    // --- User ---
    public static final String USER_NOT_FOUND = "USER_001";  /** Không tìm thấy user theo id/email/username. */
    public static final String EMAIL_ALREADY_EXISTS = "USER_002";  /** Email đã được đăng ký. */
    public static final String USERNAME_ALREADY_EXISTS = "USER_003"; /** Username đã được sử dụng. */
    public static final String USER_INACTIVE = "USER_004"; /** Tài khoản đã bị vô hiệu hóa (inactive/deleted). */

    // --- Role & Permission ---
    public static final String ROLE_NOT_FOUND = "ROLE_001"; /** Không tìm thấy vai trò theo id. */
    public static final String PERMISSION_NOT_FOUND = "PERM_001"; /** Không tìm thấy quyền theo id. */
    public static final String PERMISSION_ALREADY_EXISTS = "PERM_002"; /** Mã quyền đã tồn tại. */

    // --- Workspace ---
    public static final String WORKSPACE_NOT_FOUND = "WS_001"; /** Không tìm thấy workspace. */
    public static final String WORKSPACE_ACCESS_DENIED = "WS_002"; /** Không có quyền truy cập workspace. */
    public static final String WORKSPACE_MEMBER_NOT_FOUND = "WS_003"; /** Không tìm thấy thành viên workspace. */
    public static final String WORKSPACE_MEMBER_ALREADY_EXISTS = "WS_004"; /** User đã là thành viên workspace. */
    public static final String WORKSPACE_NAME_ALREADY_EXISTS = "WS_005"; /** Tên workspace đã tồn tại. */

    // --- Team ---
    public static final String TEAM_NOT_FOUND = "TEAM_001"; /** Không tìm thấy nhóm làm việc. */
    public static final String TEAM_ACCESS_DENIED = "TEAM_002"; /** Không có quyền truy cập nhóm. */
    public static final String TEAM_MEMBER_NOT_FOUND = "TEAM_003"; /** Không tìm thấy thành viên nhóm. */
    public static final String TEAM_MEMBER_ALREADY_EXISTS = "TEAM_004"; /** Thành viên đã có trong nhóm. */

    // --- Project ---
    public static final String PROJECT_NOT_FOUND = "PROJ_001"; /** Không tìm thấy dự án. */
    public static final String PROJECT_ACCESS_DENIED = "PROJ_002"; /** Không có quyền truy cập dự án. */
    public static final String PROJECT_MEMBER_NOT_FOUND = "PROJ_003"; /** Không tìm thấy thành viên dự án. */
    public static final String PROJECT_MEMBER_ALREADY_EXISTS = "PROJ_004"; /** Thành viên đã có trong dự án. */
    public static final String PROJECT_CODE_ALREADY_EXISTS = "PROJ_005"; /** Mã dự án đã tồn tại. */

    // --- Attachment ---
    public static final String ATTACHMENT_NOT_FOUND = "ATT_001"; /** Không tìm thấy tài liệu đính kèm. */
    public static final String ATTACHMENT_ACCESS_DENIED = "ATT_002"; /** Không có quyền thao tác tệp đính kèm. */

    // --- Product Backlog ---
    public static final String BACKLOG_NOT_FOUND = "PBL_001"; /** Không tìm thấy product backlog. */
    public static final String PBI_NOT_FOUND = "PBL_002"; /** Không tìm thấy Product Backlog Item. */
    public static final String PBI_CANNOT_DELETE = "PBL_003"; /** Không thể xóa PBI (trạng thái hoặc có task). */
    public static final String PBI_INVALID_STATUS = "PBL_004"; /** Chuyển trạng thái PBI không hợp lệ. */

    // --- Task & Workflow ---
    public static final String TASK_NOT_FOUND = "TSK_001"; /** Không tìm thấy task. */
    public static final String TASK_CANNOT_DELETE = "TSK_002"; /** Không thể xóa task. */
    public static final String TASK_INVALID_STATUS = "TSK_003"; /** Chuyển trạng thái task không hợp lệ. */
    public static final String TASK_ACCESS_DENIED = "TSK_004"; /** Không có quyền thao tác task. */
    public static final String WORKFLOW_STATE_NOT_FOUND = "WFL_001"; /** Không tìm thấy trạng thái workflow. */
    public static final String WORKFLOW_STATE_IN_USE = "WFL_002"; /** Trạng thái workflow đang được sử dụng. */
    public static final String WORKFLOW_TRANSITION_NOT_FOUND = "WFL_003"; /** Không tìm thấy transition. */
    public static final String WORKFLOW_TRANSITION_INVALID = "WFL_004"; /** Transition không hợp lệ. */

    // --- Sprint ---
    public static final String SPRINT_NOT_FOUND = "SPR_001"; /** Không tìm thấy sprint. */
    public static final String SPRINT_INVALID_STATUS = "SPR_002"; /** Chuyển trạng thái sprint không hợp lệ. */
    public static final String SPRINT_ALREADY_ACTIVE = "SPR_003"; /** Dự án đã có sprint đang hoạt động. */
    public static final String SPRINT_PBI_INVALID = "SPR_004"; /** PBI không thể thêm vào hoặc gỡ khỏi sprint. */
    public static final String SPRINT_CANNOT_COMPLETE = "SPR_005"; /** Sprint còn công việc chưa kết thúc. */
    public static final String SPRINT_ACCESS_DENIED = "SPR_006"; /** Không có quyền xem tiến độ sprint. */

    // --- Comment ---
    public static final String COMMENT_NOT_FOUND = "CMT_001"; /** Không tìm thấy bình luận. */
    public static final String COMMENT_ACCESS_DENIED = "CMT_002"; /** Không có quyền thao tác bình luận. */

    // --- Dashboard ---
    public static final String DASHBOARD_ACCESS_DENIED = "DSH_001"; /** Không có quyền xem dashboard. */

}
