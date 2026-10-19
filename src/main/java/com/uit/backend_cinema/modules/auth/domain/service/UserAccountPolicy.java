package com.uit.backend_cinema.modules.auth.domain.service;

import com.uit.backend_cinema.common.exception.BusinessException;
import com.uit.backend_cinema.common.exception.ErrorCode;
import com.uit.backend_cinema.modules.auth.domain.entity.User;

/** Shared account boundary; system records must never become interactive users. */
public final class UserAccountPolicy {
    private UserAccountPolicy() {
    }

    public static void requireInteractive(User user) {
        if (user == null || !user.isInteractiveAccount()) {
            throw new BusinessException("Tài khoản không hợp lệ", ErrorCode.INVALID_CREDENTIALS);
        }
    }
}
