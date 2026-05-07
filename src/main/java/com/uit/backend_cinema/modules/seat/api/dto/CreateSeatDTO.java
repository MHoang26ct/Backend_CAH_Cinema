package com.uit.backend_cinema.modules.seat.api.dto;

import lombok.Data;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

@Data
public class CreateSeatDTO {
    @NotNull(message = "ID phòng chiếu không được để trống")
    private Long roomId;

    @NotNull(message = "Hàng không được để trống")
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal row;

    @NotNull(message = "Cột không được để trống")
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal col;

    @NotNull(message = "ID loại ghế không được để trống")
    private Long seatTypeId;
}
