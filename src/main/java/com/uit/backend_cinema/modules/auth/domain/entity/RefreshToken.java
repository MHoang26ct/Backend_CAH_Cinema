package com.uit.backend_cinema.modules.auth.domain.entity;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class RefreshToken {
    private Long id;
    private String token;
    private LocalDateTime expiryDate;
    private Long userId;

    public boolean isExpired() {
        return expiryDate.isBefore(LocalDateTime.now());
    }
}
