package com.uit.backend_cinema.modules.auth.api.dto.response;

import com.uit.backend_cinema.modules.auth.api.dto.UserDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponseDTO {
    private String accessToken;
    private String refreshToken;
    private UserDTO user;
}
