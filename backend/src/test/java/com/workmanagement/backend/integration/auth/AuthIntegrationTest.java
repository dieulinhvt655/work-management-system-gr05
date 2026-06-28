package com.workmanagement.backend.integration.auth;

import com.workmanagement.backend.auth.dto.request.LogoutRequest;
import com.workmanagement.backend.auth.dto.request.RefreshTokenRequest;
import com.workmanagement.backend.integration.support.AbstractIntegrationTest;
import com.workmanagement.backend.integration.support.LoginTokens;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthIntegrationTest extends AbstractIntegrationTest {

    @Test
    void registerLoginRefreshLogout_shouldWorkEndToEnd() throws Exception {
        String suffix = uniqueId();
        LoginTokens tokens = registerAndLogin(suffix);

        mockMvc.perform(get("/api/v1/workspaces")
                        .with(bearer(tokens.accessToken())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));

        RefreshTokenRequest refreshRequest = new RefreshTokenRequest();
        refreshRequest.setRefreshToken(tokens.refreshToken());

        var refreshResult = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andReturn();

        String newRefreshToken = readData(refreshResult).get("refreshToken").asText();

        LogoutRequest logoutRequest = new LogoutRequest();
        logoutRequest.setRefreshToken(newRefreshToken);

        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        RefreshTokenRequest revokedRefreshRequest = new RefreshTokenRequest();
        revokedRefreshRequest.setRefreshToken(newRefreshToken);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(revokedRefreshRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void loginWithWrongPassword_shouldReturn401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"wrong-password"}
                                """.formatted(ADMIN_EMAIL)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void protectedEndpointWithoutToken_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/workspaces"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminLogin_shouldReturnAccessAndRefreshToken() throws Exception {
        LoginTokens tokens = loginAsAdmin();

        mockMvc.perform(get("/api/v1/roles")
                        .with(bearer(tokens.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }
}
