package com.uit.backend_cinema.modules.showtime.api.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CancelByRoomRequestDTO {

    @NotNull(message = "ID phòng chiếu không được trống")
    private Long roomId;

    @NotNull(message = "Ngày bắt đầu không được trống")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fromDate;

    @NotNull(message = "Ngày kết thúc không được trống")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate toDate;

    /** Lý do hủy (sẽ hiển thị trong email thông báo cho khách) */
    private String reason;
}
