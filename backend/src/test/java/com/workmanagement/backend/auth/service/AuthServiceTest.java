package com.workmanagement.backend.auth.service;

import com.workmanagement.backend.auth.dto.request.ForgotPasswordRequest;
import com.workmanagement.backend.auth.dto.request.LoginRequest;
import com.workmanagement.backend.auth.dto.request.RegisterRequest;
import com.workmanagement.backend.auth.dto.request.ResetPasswordRequest;
import com.workmanagement.backend.auth.dto.response.LoginResponse;
import com.workmanagement.backend.auth.dto.response.RegisterResponse;
import com.workmanagement.backend.auth.mapper.AuthMapper;
import com.workmanagement.backend.common.constant.ErrorCode;
import com.workmanagement.backend.common.enums.RoleScope;
import com.workmanagement.backend.common.enums.UserStatus;
import com.workmanagement.backend.common.exception.BusinessException;
import com.workmanagement.backend.security.entity.Permission;
import com.workmanagement.backend.security.entity.Role;
import com.workmanagement.backend.security.jwt.JwtProperties;
import com.workmanagement.backend.security.jwt.JwtTokenProvider;
import com.workmanagement.backend.security.repository.RolePermissionRepository;
import com.workmanagement.backend.user.entity.User;
import com.workmanagement.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RolePermissionRepository rolePermissionRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private JwtProperties jwtProperties;
    @Mock
    private AuthMapper authMapper;

    @InjectMocks
    private AuthService authService;

    private User activeUser;

    @BeforeEach
    void setUp() {
        Role role = Role.builder().id(1L).name("System Admin").scope(RoleScope.SYSTEM).build();
        activeUser = User.builder()
                .id(1L)
                .fullName("Admin")
                .email("admin@test.com")
                .username("admin")
                .passwordHash("encoded")
                .status(UserStatus.ACTIVE)
                .role(role)
                .build();
    }

    @Test
    void login_shouldReturnTokenWhenCredentialsValid() {
        LoginRequest request = new LoginRequest();
        request.setEmail("admin@test.com");
        request.setPassword("secret");

        Permission permission = Permission.builder().id(1L).code("role:read").name("Read").module("role").build();
        LoginResponse expected = LoginResponse.builder().accessToken("jwt-token").build();

        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("secret", "encoded")).thenReturn(true);
        when(rolePermissionRepository.findPermissionsByRoleId(1L)).thenReturn(List.of(permission));
        when(jwtTokenProvider.generateAccessToken(1L, List.of("role:read"))).thenReturn("jwt-token");
        when(jwtProperties.getExpiration()).thenReturn(86_400_000L);
        when(authMapper.toLoginResponse(eq(activeUser), eq("jwt-token"), eq(86_400_000L), any()))
                .thenReturn(expected);

        LoginResponse result = authService.login(request);

        assertThat(result.getAccessToken()).isEqualTo("jwt-token");
    }

    @Test
    void login_shouldThrowWhenEmailNotFound() {
        LoginRequest request = new LoginRequest();
        request.setEmail("missing@test.com");
        request.setPassword("secret");

        when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    void login_shouldThrowWhenUserInactive() {
        activeUser.setStatus(UserStatus.INACTIVE);
        LoginRequest request = new LoginRequest();
        request.setEmail("admin@test.com");
        request.setPassword("secret");

        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(activeUser));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_INACTIVE);
    }

    @Test
    void login_shouldThrowWhenPasswordWrong() {
        LoginRequest request = new LoginRequest();
        request.setEmail("admin@test.com");
        request.setPassword("wrong");

        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    void register_shouldCreateUser() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("New User");
        request.setEmail("new@test.com");
        request.setUsername("newuser");
        request.setPassword("pass123");

        User saved = User.builder().id(2L).fullName("New User").email("new@test.com").username("newuser").build();
        RegisterResponse expected = RegisterResponse.builder().id(2L).email("new@test.com").build();

        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(passwordEncoder.encode("pass123")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenReturn(saved);
        when(authMapper.toRegisterResponse(saved)).thenReturn(expected);

        RegisterResponse result = authService.register(request);

        assertThat(result.getId()).isEqualTo(2L);
        assertThat(result.getEmail()).isEqualTo("new@test.com");
    }

    @Test
    void register_shouldThrowWhenEmailExists() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("exists@test.com");
        request.setUsername("user");

        when(userRepository.existsByEmail("exists@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS);
    }

    @Test
    void forgotPassword_shouldGenerateTokenWhenUserExists() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("admin@test.com");

        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(activeUser));
        when(jwtTokenProvider.generatePasswordResetToken("admin@test.com")).thenReturn("reset-token");

        authService.forgotPassword(request);

        verify(jwtTokenProvider).generatePasswordResetToken("admin@test.com");
    }

    @Test
    void forgotPassword_shouldNotGenerateTokenWhenUserMissing() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("missing@test.com");

        when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

        authService.forgotPassword(request);

        verify(jwtTokenProvider, never()).generatePasswordResetToken(any());
    }

    @Test
    void resetPassword_shouldUpdatePasswordWhenTokenValid() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("reset-token");
        request.setNewPassword("newpass123");

        when(jwtTokenProvider.isPasswordResetToken("reset-token")).thenReturn(true);
        when(jwtTokenProvider.getEmailFromToken("reset-token")).thenReturn("admin@test.com");
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.encode("newpass123")).thenReturn("new-encoded");

        authService.resetPassword(request);

        assertThat(activeUser.getPasswordHash()).isEqualTo("new-encoded");
        verify(userRepository).save(activeUser);
    }

    @Test
    void resetPassword_shouldThrowWhenNotResetToken() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("access-token");
        request.setNewPassword("newpass123");

        when(jwtTokenProvider.isPasswordResetToken("access-token")).thenReturn(false);

        assertThatThrownBy(() -> authService.resetPassword(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.TOKEN_INVALID);
    }

}
