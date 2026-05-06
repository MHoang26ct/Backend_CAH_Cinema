package com.uit.backend_cinema.modules.seat.domain.entity;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter
public class Seat {

    private Long seatId;
    private Long roomId;
    private BigDecimal seatRow;
    private BigDecimal seatCol;
    private SeatType seatType;
    private SeatStatus status;
    private Boolean isDeleted;

    // Field này KHÔNG có trong DB, lấy từ Redis
    private Boolean isLocked;
}
