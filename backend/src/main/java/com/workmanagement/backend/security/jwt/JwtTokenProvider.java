package com.workmanagement.backend.security.jwt;

import com.workmanagement.backend.common.constant.ErrorCode;
import com.workmanagement.backend.common.exception.BusinessException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private static final String CLAIM_TYPE = "type";
    private static final String CLAIM_PERMISSIONS = "permissions";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_PASSWORD_RESET = "password_reset";

    private final JwtProperties jwtProperties;

    public String generateAccessToken(Long userId, List<String> permissions) {
        return buildToken(String.valueOf(userId), TYPE_ACCESS, jwtProperties.getExpiration(), permissions);
    }

    public String generatePasswordResetToken(String email) {
        return buildToken(email, TYPE_PASSWORD_RESET, jwtProperties.getResetExpiration(), null);
    }

    public void validateToken(String token) {
        try {
            parseClaims(token);
        } catch (ExpiredJwtException ex) {
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED, "Token đã hết hạn");
        } catch (JwtException | IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID, "Token không hợp lệ");
        }
    }

    public boolean isValidToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    public boolean isAccessToken(String token) {
        return TYPE_ACCESS.equals(parseClaims(token).get(CLAIM_TYPE, String.class));
    }

    public boolean isPasswordResetToken(String token) {
        return TYPE_PASSWORD_RESET.equals(parseClaims(token).get(CLAIM_TYPE, String.class));
    }

    public Long getUserIdFromToken(String token) {
        return Long.parseLong(parseClaims(token).getSubject());
    }

    public String getEmailFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    @SuppressWarnings("unchecked")
    public List<String> getPermissionsFromToken(String token) {
        Object permissions = parseClaims(token).get(CLAIM_PERMISSIONS);
        if (permissions instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of();
    }

    private String buildToken(String subject, String type, long expirationMs, List<String> permissions) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        var builder = Jwts.builder()
                .subject(subject)
                .claim(CLAIM_TYPE, type)
                .issuedAt(now)
                .expiration(expiry);

        if (permissions != null) {
            builder.claim(CLAIM_PERMISSIONS, permissions);
        }

        return builder.signWith(getSigningKey()).compact();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

}
