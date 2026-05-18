package com.uit.backend_cinema.modules.auth.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import lombok.Data;
import org.hibernate.validator.constraints.URL;

@Data
public class UpdateProfileRequestDTO {

    @Size(min = 2, max = 100, message = "Tên phải từ 2 đến 100 ký tự")
    private String name;

    @Email(message = "Email không hợp lệ")
    private String email;

    @Size(min = 9, max = 11, message = "Số điện thoại phải từ 9 đến 11 ký tự")
    private String phone;

    @URL(message = "URL avatar không hợp lệ")
    private String avatarUrl;
}
