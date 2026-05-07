package com.uit.backend_cinema.modules.seat.domain.entity;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

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

    // Field này KHÔNG có trong DB, lấy từ tickets theo showtime
    private Boolean isSold;

    // AVAILABLE, LOCKED, SOLD để FE phân biệt trạng thái giữ tạm và đã bán
    private String occupancyStatus;
}
