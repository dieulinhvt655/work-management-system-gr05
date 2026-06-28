package com.workmanagement.backend.auth.service;

import com.workmanagement.backend.auth.dto.request.ForgotPasswordRequest;
import com.workmanagement.backend.auth.dto.request.LoginRequest;
import com.workmanagement.backend.auth.dto.request.LogoutRequest;
import com.workmanagement.backend.auth.dto.request.RefreshTokenRequest;
import com.workmanagement.backend.auth.dto.request.RegisterRequest;
import com.workmanagement.backend.auth.dto.request.ResetPasswordRequest;
import com.workmanagement.backend.auth.dto.response.LoginResponse;
import com.workmanagement.backend.auth.dto.response.RegisterResponse;
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
    private final PasswordResetEmailService passwordResetEmailService;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS, "Email hoặc mật khẩu không đúng"));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.USER_INACTIVE, "Tài khoản đã bị vô hiệu hóa");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "Email hoặc mật khẩu không đúng");
        }

        List<Permission> permissions = loadPermissions(user);
        List<String> permissionCodes = permissions.stream().map(Permission::getCode).toList();
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), permissionCodes);
        String refreshToken = refreshTokenService.createRefreshToken(user);

        return authMapper.toLoginResponse(
                user,
                accessToken,
                refreshToken,
                jwtProperties.getExpiration(),
                permissions
        );
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS, "Email đã được đăng ký");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException(ErrorCode.USERNAME_ALREADY_EXISTS, "Username đã được sử dụng");
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .status(UserStatus.ACTIVE)
                .build();

        return authMapper.toRegisterResponse(userRepository.save(user));
    }

    @Transactional
    public TokenResponse refresh(RefreshTokenRequest request) {
        RefreshToken storedToken = refreshTokenService.validate(request.getRefreshToken());
        User user = storedToken.getUser();

        List<Permission> permissions = loadPermissions(user);
        List<String> permissionCodes = permissions.stream().map(Permission::getCode).toList();
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), permissionCodes);
        String newRefreshToken = refreshTokenService.rotate(storedToken);

        return authMapper.toTokenResponse(accessToken, newRefreshToken, jwtProperties.getExpiration());
    }

    @Transactional
    public void logout(LogoutRequest request) {
        refreshTokenService.revoke(request.getRefreshToken());
    }

    @Transactional(readOnly = true)
    public void forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            String resetToken = jwtTokenProvider.generatePasswordResetToken(user.getEmail());
            passwordResetEmailService.sendResetLink(user.getEmail(), resetToken);
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
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

}
