package com.uit.backend_cinema.modules.cinema.api.entity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.Data;

@Data
public class UpdateCinemaRequestDTO {

    @NotNull(message = "ID rạp không được trống")
    private Long cinemaId;

    @NotBlank(message = "Tên rạp không được trống")
    private String name;

    @NotBlank(message = "Địa chỉ không được trống")
    private String address;

    private String hotline;
}
