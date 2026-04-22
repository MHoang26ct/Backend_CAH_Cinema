package com.uit.backend_cinema.modules.auth.api.dto;

import lombok.Data;

@Data
public class RefreshTokenDTO {
    private String refreshToken; //Nhận từ mobile app
}
