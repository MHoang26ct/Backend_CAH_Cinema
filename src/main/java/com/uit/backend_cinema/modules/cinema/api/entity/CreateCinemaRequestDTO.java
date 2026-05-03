package com.uit.backend_cinema.modules.cinema.api.entity;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateCinemaRequestDTO {

    @NotBlank(message = "Tên rạp không được trống")
    private String name;

    @NotBlank(message = "Địa chỉ không được trống")
    private String address;

    private String hotline;
}
