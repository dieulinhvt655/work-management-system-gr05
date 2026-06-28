package com.workmanagement.backend.common.util;

import com.workmanagement.backend.common.constant.ErrorCode;
import com.workmanagement.backend.common.exception.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Chưa đăng nhập");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof Long userId) {
            return userId;
        }

        throw new BusinessException(ErrorCode.UNAUTHORIZED, "Chưa đăng nhập");
    }

}
