package com.uit.backend_cinema.modules.auth.api.dto.response;

import jakarta.validation.constraints.NotNull;

import lombok.Data;

@Data
public class FP_VerifyOtpResponseOTP {
    @NotNull
    String token;
}
