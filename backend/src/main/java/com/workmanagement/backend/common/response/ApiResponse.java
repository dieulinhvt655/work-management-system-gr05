package com.workmanagement.backend.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
//là wrapper chuẩn hóa mọi response JSON từ API 
// giúp frontend luôn nhận cùng một cấu trúc, dù thành công hay lỗi.
public class ApiResponse<T> {

    private final boolean success;
    private final T data;
    private final String message;
    private final String errorCode;

    // thành công, trả data - lấy user có data
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, null);
    }

    // thành công, trả data và message - tạo user thành công - có data + message
    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, data, message, null);
    }

    // thành công, không trả data - xoá user thành công - không cần data
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(true, null, null, null);
    }
    // lỗi, trả message và errorCode - lỗi từ service hoặc handler
    public static <T> ApiResponse<T> error(String errorCode, String message) {
        return new ApiResponse<>(false, null, message, errorCode);
    }

}
