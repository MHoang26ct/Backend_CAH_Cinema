package com.uit.backend_cinema.modules.seat.api.controller;

import com.uit.backend_cinema.common.exception.BusinessException;
import com.uit.backend_cinema.common.exception.ErrorCode;
import com.uit.backend_cinema.common.sercurity.SecurityUtil;
import com.uit.backend_cinema.common.util.ApiResponse;
import com.uit.backend_cinema.modules.auth.domain.repository.UserRepository;
import com.uit.backend_cinema.modules.seat.domain.service.SeatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/seats")
public class AuthSeatController {

    private final SeatService seatService;
    private final UserRepository userRepository;

    public AuthSeatController(SeatService seatService, UserRepository userRepository) {
        this.seatService = seatService;
        this.userRepository = userRepository;
    }

    // User chọn ghế → lock Redis 10 phút
    // POST /api/v1/seats/{seatId}/lock?showtimeId=5
    @PostMapping("/{seatId}/lock")
    public ResponseEntity<?> lockSeat(
            @PathVariable Long seatId,
            @RequestParam Long showtimeId
    ) {
        Long userId = getCurrentUserId();
        boolean success = seatService.selectSeat(showtimeId, seatId, userId);
        if (!success) {
            throw new BusinessException("Ghế đang được người khác chọn", ErrorCode.SEAT_ALREADY_BOOKED);
        }
        return ResponseEntity.ok(ApiResponse.success(null, "Đã chọn ghế thành công"));
    }

    // User bỏ chọn ghế → unlock Redis
    // DELETE /api/v1/seats/{seatId}/lock?showtimeId=5
    @DeleteMapping("/{seatId}/lock")
    public ResponseEntity<?> unlockSeat(
            @PathVariable Long seatId,
            @RequestParam Long showtimeId
    ) {
        Long userId = getCurrentUserId();
        seatService.deselectSeat(showtimeId, seatId, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Đã bỏ chọn ghế"));
    }

    private Long getCurrentUserId() {
        String email = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new BusinessException("Chưa đăng nhập", ErrorCode.UNAUTHORIZED));
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("Không tìm thấy user", ErrorCode.RESOURCE_NOT_FOUND))
                .getUserId();
    }
}
