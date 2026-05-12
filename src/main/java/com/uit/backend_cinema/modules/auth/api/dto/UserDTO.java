package com.uit.backend_cinema.modules.auth.api.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class UserDTO {
    private Long userId;
    private String name;
    private String email;
    private String phone;
    private String avatarUrl;
    private String authProvider;
    private String role;
    private BigDecimal totalPaid;
    private Integer totalPoint;
    private String rankLevel;
}
