package com.uit.backend_cinema.modules.seat.api.controller;

import com.uit.backend_cinema.common.exception.BusinessException;
import com.uit.backend_cinema.common.exception.ErrorCode;
import com.uit.backend_cinema.common.util.ApiResponse;
import com.uit.backend_cinema.modules.seat.api.dto.SeatDTO;
import com.uit.backend_cinema.modules.seat.api.mapper.SeatApiMapper;
import com.uit.backend_cinema.modules.seat.domain.service.SeatService;
import com.uit.backend_cinema.modules.showtime.domain.entity.Showtime;
import com.uit.backend_cinema.modules.showtime.domain.service.ShowtimeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/public/seats")
public class OpenSeatController {
    private final SeatService seatService;
    private final SeatApiMapper seatApiMapper;
    private final ShowtimeService showtimeService;

    public OpenSeatController(SeatService seatService, SeatApiMapper seatApiMapper,
                              ShowtimeService showtimeService) {
        this.seatService = seatService;
        this.seatApiMapper = seatApiMapper;
        this.showtimeService = showtimeService;
    }

    // Lấy danh sách ghế theo suất chiếu (kèm trạng thái lock)
    // GET /api/v1/public/seats?showtimeId=5
    @GetMapping
    public ResponseEntity<?> getSeatsByShowtime(@RequestParam Long showtimeId) {
        Showtime showtime = showtimeService.getById(showtimeId);
        List<SeatDTO> seats = seatService.getSeatsByRoomId(showtime.getRoomId(), showtimeId)
                .stream()
                .map(seatApiMapper::toDto)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(seats));
    }
}
