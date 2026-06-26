package com.workmanagement.backend.common.exception;

import com.workmanagement.backend.common.constant.ErrorCode;
import com.workmanagement.backend.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Bắt exception tập trung và trả {@link ApiResponse} JSON thống nhất cho mọi API.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Lỗi nghiệp vụ do service throw {@link BusinessException}. */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
        HttpStatus status = resolveStatus(ex.getErrorCode());
        return ResponseEntity
                .status(status)
                .body(ApiResponse.error(ex.getErrorCode(), ex.getMessage()));
    }

    /** Lỗi validation khi request DTO không pass {@code @Valid}. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error(ErrorCode.VALIDATION_ERROR, message));
    }

    /** Lỗi không có quyền truy cập từ Spring Security. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ErrorCode.UNAUTHORIZED, "Không có quyền truy cập"));
    }

    /** Lỗi hệ thống không lường trước (bug, DB down, ...). */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity
                .internalServerError()
                .body(ApiResponse.error(ErrorCode.INTERNAL_ERROR, "Đã xảy ra lỗi hệ thống"));
    }

    /** Map {@code errorCode} sang HTTP status tương ứng. */
    private HttpStatus resolveStatus(String errorCode) {
        return switch (errorCode) {
            case ErrorCode.USER_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case ErrorCode.INVALID_CREDENTIALS,
                 ErrorCode.TOKEN_EXPIRED,
                 ErrorCode.TOKEN_INVALID,
                 ErrorCode.UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case ErrorCode.USER_INACTIVE -> HttpStatus.FORBIDDEN;
            default -> HttpStatus.BAD_REQUEST;
        };
    }

}
