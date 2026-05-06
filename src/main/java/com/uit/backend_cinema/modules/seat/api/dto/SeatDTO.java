package com.uit.backend_cinema.modules.seat.api.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SeatDTO {
    private Long seatId;

    // Tọa độ thô — dùng để positioning ghế trên Grid (Mobile/FE)
    private BigDecimal row;
    private BigDecimal col;

    // Nhãn hiển thị — null nếu là đường đi (AISLE)
    // rowLabel: "A", "B", "C"... (null nếu row = x.5)
    // colLabel: "1", "2", "3"... (null nếu col = x.5)
    private String rowLabel;
    private String colLabel;

    private SeatTypeDTO seatType;
    private String status;     // ACTIVE, PREPARE
    private Boolean isLocked;  // true = đang có người giữ (từ Redis)
}
