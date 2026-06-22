package com.uit.backend_cinema.modules.ticket.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CheckInRequestDTO {
    @NotBlank(message = "Mã QR token không được để trống")
    private String qrToken;
}
