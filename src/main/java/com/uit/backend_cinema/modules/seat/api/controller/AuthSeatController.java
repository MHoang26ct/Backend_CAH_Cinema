package com.uit.backend_cinema.modules.seat.api.controller;

import com.uit.backend_cinema.common.exception.BusinessException;
import com.uit.backend_cinema.common.exception.ErrorCode;
import com.uit.backend_cinema.common.sercurity.CustomUserDetails;
import com.uit.backend_cinema.common.util.ApiResponse;
import com.uit.backend_cinema.modules.seat.api.dto.SeatBatchLockRequestDTO;
import com.uit.backend_cinema.modules.seat.domain.service.SeatService;
import com.uit.backend_cinema.modules.showtime.domain.entity.Showtime;
import com.uit.backend_cinema.modules.showtime.domain.service.ShowtimeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/seats")
public class AuthSeatController {

    private final SeatService seatService;
    private final ShowtimeService showtimeService;

    public AuthSeatController(SeatService seatService, ShowtimeService showtimeService) {
        this.seatService = seatService;
        this.showtimeService = showtimeService;
    }

    // User chọn ghế → lock Redis 10 phút
    // POST /api/v1/seats/{seatId}/lock?showtimeId=5
    @PostMapping("/{seatId}/lock")
    public ResponseEntity<?> lockSeat(
            @PathVariable Long seatId,
            @RequestParam Long showtimeId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        Showtime showtime = showtimeService.getById(showtimeId);
        boolean success = seatService.preLockSeats(showtimeId, java.util.List.of(seatId), showtime.getRoomId(), user.getUserId());
        if (!success) {
            throw new BusinessException("Ghế đang được người khác chọn", ErrorCode.SEAT_ALREADY_BOOKED);
        }
        return ResponseEntity.ok(ApiResponse.success(null, "Đã chọn ghế thành công"));
    }

    @PostMapping("/pre-lock")
    public ResponseEntity<?> preLockSeats(
            @Valid @RequestBody SeatBatchLockRequestDTO requestDTO,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        Showtime showtime = showtimeService.getById(requestDTO.getShowtimeId());
        boolean success = seatService.preLockSeats(
                requestDTO.getShowtimeId(),
                requestDTO.getSeatIds(),
                showtime.getRoomId(),
                user.getUserId()
        );
        if (!success) {
            throw new BusinessException("Một hoặc nhiều ghế đang được người khác giữ", ErrorCode.SEAT_ALREADY_BOOKED);
        }
        return ResponseEntity.ok(ApiResponse.success(null, "Đã giữ ghế tạm thời thành công"));
    }

    // User bỏ chọn ghế → unlock Redis
    // DELETE /api/v1/seats/{seatId}/lock?showtimeId=5
    @DeleteMapping("/{seatId}/lock")
    public ResponseEntity<?> unlockSeat(
            @PathVariable Long seatId,
            @RequestParam Long showtimeId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        seatService.deselectSeat(showtimeId, seatId, user.getUserId());
        return ResponseEntity.ok(ApiResponse.success(null, "Đã bỏ chọn ghế"));
    }
}
