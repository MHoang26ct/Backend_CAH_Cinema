package com.uit.backend_cinema.modules.auth.domain.entity;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

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
