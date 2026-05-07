package com.uit.backend_cinema.modules.cinema.api.entity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.Data;

@Data
public class CreateRoomRequestDTO {

    @NotNull(message = "ID rạp không được trống")
    private Long cinemaId;

    @NotBlank(message = "Tên phòng không được trống")
    private String roomName;
}
