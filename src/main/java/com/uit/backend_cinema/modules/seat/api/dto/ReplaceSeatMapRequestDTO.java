package com.uit.backend_cinema.modules.seat.api.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReplaceSeatMapRequestDTO {

    @NotNull(message = "ID phòng chiếu không được trống")
    private Long roomId;

    @NotEmpty(message = "Danh sách ghế không được trống")
    @Valid
    private List<CreateSeatDTO> seats;
}
