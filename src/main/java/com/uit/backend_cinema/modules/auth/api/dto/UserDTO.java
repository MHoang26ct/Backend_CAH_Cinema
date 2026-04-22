package com.uit.backend_cinema.modules.auth.api.dto;

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
}
