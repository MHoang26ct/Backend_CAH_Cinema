package com.uit.backend_cinema.modules.auth.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GoogleLoginRequestDTO {
    @NotBlank(message = "ID Token không được để trống")
    private String idToken;
}
