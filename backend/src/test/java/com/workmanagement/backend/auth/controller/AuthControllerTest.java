package com.workmanagement.backend.auth.controller;

import com.workmanagement.backend.auth.dto.request.LoginRequest;
import com.workmanagement.backend.auth.dto.request.RegisterRequest;
import com.workmanagement.backend.auth.dto.response.LoginResponse;
import com.workmanagement.backend.auth.dto.response.RegisterResponse;
import com.workmanagement.backend.auth.service.AuthService;
import com.workmanagement.backend.common.constant.ErrorCode;
import com.workmanagement.backend.common.exception.BusinessException;
import com.workmanagement.backend.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void login_shouldReturn200WithToken() throws Exception {
        LoginResponse response = LoginResponse.builder()
                .accessToken("jwt-token")
                .tokenType("Bearer")
                .expiresIn(86_400_000L)
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"admin@test.com","password":"secret"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("jwt-token"));
    }

    @Test
    void login_shouldReturn401WhenCredentialsInvalid() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BusinessException(ErrorCode.INVALID_CREDENTIALS, "Email hoặc mật khẩu không đúng"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"admin@test.com","password":"wrong"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.INVALID_CREDENTIALS));
    }

    @Test
    void register_shouldReturnSuccessPayload() throws Exception {
        RegisterResponse response = RegisterResponse.builder()
                .id(1L)
                .email("test@test.com")
                .username("testuser")
                .fullName("Test")
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Test","email":"test@test.com","username":"testuser","password":"pass123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("test@test.com"));
    }

    @Test
    void login_shouldReturn400WhenEmailInvalid() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"not-an-email","password":"secret"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.VALIDATION_ERROR));
    }

}
