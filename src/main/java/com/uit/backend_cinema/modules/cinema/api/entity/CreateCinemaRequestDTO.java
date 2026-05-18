package com.uit.backend_cinema.modules.cinema.api.entity;

import org.hibernate.validator.constraints.URL;

import jakarta.validation.constraints.NotBlank;

import lombok.Data;

@Data
public class CreateCinemaRequestDTO {

    @NotBlank(message = "Tên rạp không được trống")
    private String name;

    @NotBlank(message = "Địa chỉ không được trống")
    private String address;

    @URL(message = "URL không hợp lệ")
    private String imageUrl;

    private String hotline;
}
