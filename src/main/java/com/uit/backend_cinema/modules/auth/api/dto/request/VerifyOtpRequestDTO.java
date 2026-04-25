package com.uit.backend_cinema.modules.auth.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.checkerframework.common.value.qual.MinLen;

@Data
public class VerifyOtpRequestDTO {
    @MinLen(6)
    private String otp;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String email;
}
