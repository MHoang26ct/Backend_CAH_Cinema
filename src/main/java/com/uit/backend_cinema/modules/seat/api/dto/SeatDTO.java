package com.uit.backend_cinema.modules.seat.api.dto;

import lombok.Data;

@Data
public class SeatDTO {
    private Long seatId;
    private String seatRow;
    private Integer seatNumber;
    private SeatTypeDTO seatType;
    private String status;    // ACTIVE, PREPARE
    private Boolean isLocked; // true = đang có người đặt (từ Redis)
}
