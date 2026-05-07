package com.uit.backend_cinema.modules.auth.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import lombok.Data;

@Data
public class ChangePasswordForgetRequestDTO {
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    private String newPassword;

    @NotBlank(message = "Reset Token không được để trống")
    private String resetToken;
}
