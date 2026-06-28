package com.workmanagement.backend.auth.controller;

import com.workmanagement.backend.auth.dto.request.ForgotPasswordRequest;
import com.workmanagement.backend.auth.dto.request.LoginRequest;
import com.workmanagement.backend.auth.dto.request.RegisterRequest;
import com.workmanagement.backend.auth.dto.request.ResetPasswordRequest;
import com.workmanagement.backend.auth.dto.response.LoginResponse;
import com.workmanagement.backend.auth.dto.response.RegisterResponse;
import com.workmanagement.backend.auth.service.AuthService;
import com.workmanagement.backend.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** UC-1.1 — Đăng nhập */
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    /** UC-1.1 — Đăng ký tài khoản (Guest User) */
    @PostMapping("/register")
    public ApiResponse<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success(authService.register(request), "Đăng ký thành công");
    }

    /** UC-1.2 — Đăng xuất */
    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        authService.logout();
        return ApiResponse.success(null, "Đăng xuất thành công");
    }

    /** UC-1.3 — Yêu cầu khôi phục mật khẩu */
    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ApiResponse.success(null, "Nếu email tồn tại, hướng dẫn khôi phục đã được gửi");
    }

    /** UC-1.3 — Đặt lại mật khẩu bằng reset token */
    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ApiResponse.success(null, "Đặt lại mật khẩu thành công");
    }

}
