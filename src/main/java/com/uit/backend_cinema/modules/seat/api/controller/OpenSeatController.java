package com.uit.backend_cinema.modules.seat.api.controller;

import com.uit.backend_cinema.common.util.ApiResponse;
import com.uit.backend_cinema.modules.seat.api.dto.SeatDTO;
import com.uit.backend_cinema.modules.seat.api.mapper.SeatApiMapper;
import com.uit.backend_cinema.modules.seat.domain.service.SeatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/public/seats")
public class OpenSeatController {
     private final SeatService seatService;
    private final SeatApiMapper seatApiMapper;

    public OpenSeatController(SeatService seatService, SeatApiMapper seatApiMapper) {
        this.seatService = seatService;
        this.seatApiMapper = seatApiMapper;
    }

    // Lấy danh sách ghế theo suất chiếu (kèm trạng thái lock)
    // GET /api/v1/public/seats?roomId=1&showtimeId=5
    @GetMapping
    public ResponseEntity<?> getSeatsByShowtime(
            @RequestParam Long roomId,
            @RequestParam Long showtimeId
    ) {
        List<SeatDTO> seats = seatService.getSeatsByShowtime(roomId, showtimeId)
                .stream()
                .map(seatApiMapper::toDto)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(seats));
    }
}
