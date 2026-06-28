package com.workmanagement.backend.security.jwt;

import com.workmanagement.backend.common.constant.ErrorCode;
import com.workmanagement.backend.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("test-jwt-secret-key-minimum-32-characters-long");
        properties.setExpiration(3_600_000);
        properties.setResetExpiration(900_000);
        jwtTokenProvider = new JwtTokenProvider(properties);
    }

    @Test
    void generateAccessToken_shouldContainUserIdAndPermissions() {
        String token = jwtTokenProvider.generateAccessToken(42L, List.of("role:read", "user:create"));

        assertThat(jwtTokenProvider.isValidToken(token)).isTrue();
        assertThat(jwtTokenProvider.isAccessToken(token)).isTrue();
        assertThat(jwtTokenProvider.getUserIdFromToken(token)).isEqualTo(42L);
        assertThat(jwtTokenProvider.getPermissionsFromToken(token))
                .containsExactly("role:read", "user:create");
    }

    @Test
    void generatePasswordResetToken_shouldBeResetType() {
        String token = jwtTokenProvider.generatePasswordResetToken("user@test.com");

        assertThat(jwtTokenProvider.isPasswordResetToken(token)).isTrue();
        assertThat(jwtTokenProvider.isAccessToken(token)).isFalse();
        assertThat(jwtTokenProvider.getEmailFromToken(token)).isEqualTo("user@test.com");
    }

    @Test
    void validateToken_shouldRejectInvalidToken() {
        assertThatThrownBy(() -> jwtTokenProvider.validateToken("invalid.token.here"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.TOKEN_INVALID);
    }

    @Test
    void isValidToken_shouldReturnFalseForMalformedToken() {
        assertThat(jwtTokenProvider.isValidToken("not-a-jwt")).isFalse();
    }

}
