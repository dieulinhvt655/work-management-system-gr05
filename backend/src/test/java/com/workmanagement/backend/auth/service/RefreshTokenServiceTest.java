package com.workmanagement.backend.auth.service;

import com.workmanagement.backend.auth.entity.RefreshToken;
import com.workmanagement.backend.auth.repository.RefreshTokenRepository;
import com.workmanagement.backend.common.constant.ErrorCode;
import com.workmanagement.backend.common.enums.UserStatus;
import com.workmanagement.backend.common.exception.BusinessException;
import com.workmanagement.backend.security.jwt.JwtProperties;
import com.workmanagement.backend.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private JwtProperties jwtProperties;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private User activeUser;

    @BeforeEach
    void setUp() {
        activeUser = User.builder()
                .id(1L)
                .email("admin@test.com")
                .status(UserStatus.ACTIVE)
                .build();
    }

    @Test
    void createRefreshToken_shouldPersistHashedToken() {
        when(jwtProperties.getRefreshExpiration()).thenReturn(86_400_000L);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String rawToken = refreshTokenService.createRefreshToken(activeUser);

        assertThat(rawToken).isNotBlank();

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());

        RefreshToken saved = captor.getValue();
        assertThat(saved.getUser()).isEqualTo(activeUser);
        assertThat(saved.getTokenHash()).isNotEqualTo(rawToken);
        assertThat(saved.getTokenHash()).hasSize(64);
        assertThat(saved.isRevoked()).isFalse();
    }

    @Test
    void validate_shouldReturnTokenWhenValid() {
        String rawToken = "valid-refresh-token";
        RefreshToken stored = storedToken(rawToken, LocalDateTime.now().plusDays(1));

        when(refreshTokenRepository.findByTokenHashAndRevokedFalse(any())).thenReturn(Optional.of(stored));

        RefreshToken result = refreshTokenService.validate(rawToken);

        assertThat(result).isEqualTo(stored);
    }

    @Test
    void validate_shouldThrowWhenTokenMissing() {
        when(refreshTokenRepository.findByTokenHashAndRevokedFalse(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.validate("missing-token"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.REFRESH_TOKEN_INVALID);
    }

    @Test
    void validate_shouldRevokeAndThrowWhenExpired() {
        String rawToken = "expired-refresh-token";
        RefreshToken stored = storedToken(rawToken, LocalDateTime.now().minusMinutes(1));

        when(refreshTokenRepository.findByTokenHashAndRevokedFalse(any())).thenReturn(Optional.of(stored));
        when(refreshTokenRepository.save(stored)).thenReturn(stored);

        assertThatThrownBy(() -> refreshTokenService.validate(rawToken))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.TOKEN_EXPIRED);

        assertThat(stored.isRevoked()).isTrue();
    }

    @Test
    void revoke_shouldMarkTokenRevoked() {
        String rawToken = "refresh-token";
        RefreshToken stored = storedToken(rawToken, LocalDateTime.now().plusDays(1));

        when(refreshTokenRepository.findByTokenHashAndRevokedFalse(any())).thenReturn(Optional.of(stored));
        when(refreshTokenRepository.save(stored)).thenReturn(stored);

        refreshTokenService.revoke(rawToken);

        assertThat(stored.isRevoked()).isTrue();
    }

    @Test
    void revoke_shouldBeIdempotentWhenTokenMissing() {
        when(refreshTokenRepository.findByTokenHashAndRevokedFalse(any())).thenReturn(Optional.empty());

        refreshTokenService.revoke("missing-token");

        verify(refreshTokenRepository, never()).save(any());
    }

    private RefreshToken storedToken(String rawToken, LocalDateTime expiresAt) {
        return RefreshToken.builder()
                .id(1L)
                .user(activeUser)
                .tokenHash(hashLikeService(rawToken))
                .expiresAt(expiresAt)
                .revoked(false)
                .build();
    }

    private static String hashLikeService(String rawToken) {
        try {
            var digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hashed);
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

}
