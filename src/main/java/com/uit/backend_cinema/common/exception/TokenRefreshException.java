package com.uit.backend_cinema.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class TokenRefreshException extends RuntimeException {
    public TokenRefreshException(String token, String message) {
        super(String.format("Token [%s] không hợp lệ: %s", token, message));
    }
}
