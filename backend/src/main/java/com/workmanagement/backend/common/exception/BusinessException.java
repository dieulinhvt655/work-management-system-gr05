package com.workmanagement.backend.common.exception;

import lombok.Getter;

/**
 * Exception cho lỗi nghiệp vụ do service chủ động {@code throw}.
 * <p>
 * Mang {@code errorCode} (từ {@link com.workmanagement.backend.common.constant.ErrorCode})
 * và {@code message} để {@link GlobalExceptionHandler} chuyển thành {@code ApiResponse}.
 */
@Getter
public class BusinessException extends RuntimeException {

    /** Mã lỗi chuẩn trả về cho frontend, ví dụ {@code USER_002}. */
    private final String errorCode;

    /**
     * @param errorCode mã lỗi từ {@code ErrorCode}
     * @param message   mô tả lỗi hiển thị cho người dùng
     */
    public BusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

}
