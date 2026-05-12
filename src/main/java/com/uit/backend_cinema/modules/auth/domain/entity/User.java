package com.uit.backend_cinema.modules.auth.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class User {
    private Long userId;
    private String providerId;
    private String password;
    private String name;
    private String email;
    private String phone;
    private String avatarUrl;
    private AuthProvider authProvider;
    private String role; // Tên role (VD: "ROLE_USER", "ROLE_ADMIN")
    private BigDecimal totalPaid;
    private Integer totalPoint;
    private UserRank rankLevel;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isDeleted;
}
