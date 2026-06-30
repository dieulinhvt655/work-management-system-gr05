package com.workmanagement.backend.auth.service;

import com.workmanagement.backend.activitylog.constant.ActivityLogAction;
import com.workmanagement.backend.activitylog.service.ActivityLogService;
import com.workmanagement.backend.auth.dto.request.ForgotPasswordRequest;
import com.workmanagement.backend.auth.dto.request.LoginRequest;
import com.workmanagement.backend.auth.dto.request.LogoutRequest;
import com.workmanagement.backend.auth.dto.request.RefreshTokenRequest;
import com.workmanagement.backend.auth.dto.request.ResetPasswordRequest;
import com.workmanagement.backend.auth.dto.response.LoginResponse;
import com.workmanagement.backend.auth.dto.response.TokenResponse;
import com.workmanagement.backend.auth.entity.RefreshToken;
import com.workmanagement.backend.auth.mapper.AuthMapper;
import com.workmanagement.backend.common.constant.ErrorCode;
import com.workmanagement.backend.common.enums.UserStatus;
import com.workmanagement.backend.common.exception.BusinessException;
import com.workmanagement.backend.security.entity.Permission;
import com.workmanagement.backend.security.jwt.JwtProperties;
import com.workmanagement.backend.security.jwt.JwtTokenProvider;
import com.workmanagement.backend.security.repository.RolePermissionRepository;
import com.workmanagement.backend.user.entity.User;
import com.workmanagement.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final AuthMapper authMapper;
    private final RefreshTokenService refreshTokenService;
    private final ActivityLogService activityLogService;
    private final PasswordResetEmailService passwordResetEmailService;

    /** UC-1.1 — Đăng nhập, cấp access token và refresh token */
    @Transactional
    public LoginResponse login(LoginRequest request) {
        validateLoginRequest(request);

        String email = normalizeEmail(request.getEmail());
        String password = request.getPassword();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS, "Email hoặc mật khẩu không đúng"));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.USER_INACTIVE, "Tài khoản đã bị vô hiệu hóa");
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "Email hoặc mật khẩu không đúng");
        }

        List<Permission> permissions = loadPermissions(user);
        List<String> permissionCodes = permissions.stream().map(Permission::getCode).toList();
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), permissionCodes);
        String refreshToken = refreshTokenService.createRefreshToken(user);
        activityLogService.recordOrgEvent(
                user.getId(),
                ActivityLogAction.USER_LOGIN,
                ActivityLogAction.TARGET_USER,
                user.getId(),
                "SUCCESS"
        );

        return authMapper.toLoginResponse(
                user,
                accessToken,
                refreshToken,
                jwtProperties.getExpiration(),
                permissions
        );
    }
    /** UC-1.1 — Làm mới access token bằng refresh token hợp lệ */
    @Transactional
    public TokenResponse refresh(RefreshTokenRequest request) {
        validateRefreshRequest(request);

        RefreshToken storedToken = refreshTokenService.validate(request.getRefreshToken().trim());
        User user = storedToken.getUser();

        if (user == null || user.getStatus() != UserStatus.ACTIVE) {
            if (user != null && user.getId() != null) {
                refreshTokenService.revokeAllForUser(user.getId());
            }
            throw new BusinessException(ErrorCode.USER_INACTIVE, "Tài khoản đã bị vô hiệu hóa");
        }

        List<Permission> permissions = loadPermissions(user);
        List<String> permissionCodes = permissions.stream().map(Permission::getCode).toList();
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), permissionCodes);
        String newRefreshToken = refreshTokenService.rotate(storedToken);

        return authMapper.toTokenResponse(accessToken, newRefreshToken, jwtProperties.getExpiration());
    }

    /** UC-1.2 — Đăng xuất, thu hồi refresh token */
    @Transactional
    public void logout(LogoutRequest request) {
        refreshTokenService.revoke(request.getRefreshToken());
    }

    /** UC-1.3 — Gửi email khôi phục mật khẩu nếu email tồn tại */
    @Transactional(readOnly = true)
    public void forgotPassword(ForgotPasswordRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Dữ liệu khôi phục mật khẩu không được để trống");
        }
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            String resetToken = jwtTokenProvider.generatePasswordResetToken(user.getEmail());
            passwordResetEmailService.sendResetLink(user.getEmail(), resetToken);
        });
    }

    /** UC-1.3 — Đặt lại mật khẩu bằng reset token */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Dữ liệu đặt lại mật khẩu không được để trống");
        }
        jwtTokenProvider.validateToken(request.getToken());

        if (!jwtTokenProvider.isPasswordResetToken(request.getToken())) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID, "Token không hợp lệ");
        }

        String email = jwtTokenProvider.getEmailFromToken(request.getToken());
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "Không tìm thấy người dùng"));

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    private List<Permission> loadPermissions(User user) {
        if (user.getRole() == null) {
            return List.of();
        }
        return rolePermissionRepository.findPermissionsByRoleId(user.getRole().getId());
    }

    private void validateLoginRequest(LoginRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Dữ liệu đăng nhập không được để trống");
        }
        if (!StringUtils.hasText(request.getEmail())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Email không được để trống");
        }
        if (!StringUtils.hasText(request.getPassword())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Mật khẩu không được để trống");
        }
    }

    private void validateRefreshRequest(RefreshTokenRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Dữ liệu refresh token không được để trống");
        }
        if (!StringUtils.hasText(request.getRefreshToken())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Refresh token không được để trống");
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

}
