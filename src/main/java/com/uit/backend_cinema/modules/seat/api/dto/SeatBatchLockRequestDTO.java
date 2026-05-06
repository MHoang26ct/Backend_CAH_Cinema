package com.uit.backend_cinema.modules.seat.api.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class SeatBatchLockRequestDTO {
    @NotNull(message = "Showtime ID không được để trống")
    private Long showtimeId;

    @NotEmpty(message = "Danh sách ghế không được trống")
    private List<Long> seatIds;
}
