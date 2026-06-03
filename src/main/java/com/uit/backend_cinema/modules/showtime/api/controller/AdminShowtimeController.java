package com.uit.backend_cinema.modules.showtime.api.controller;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.uit.backend_cinema.common.util.ApiResponse;
import com.uit.backend_cinema.modules.booking.domain.service.BookingService;
import com.uit.backend_cinema.modules.showtime.api.dto.CancelByRoomRequestDTO;
import com.uit.backend_cinema.modules.showtime.api.dto.CreateShowtimeDTO;
import com.uit.backend_cinema.modules.showtime.api.dto.UpdateShowtimeDTO;
import com.uit.backend_cinema.modules.showtime.api.mapper.ShowtimeApiMapper;
import com.uit.backend_cinema.modules.showtime.domain.entity.Showtime;
import com.uit.backend_cinema.modules.showtime.domain.service.ShowtimeService;

@RestController
@RequestMapping("/api/v1/admin/showtime")
public class AdminShowtimeController {
    private final ShowtimeService showtimeService;
    private final ShowtimeApiMapper mapper;
    private final BookingService bookingService;

    public AdminShowtimeController(ShowtimeService showtimeService, ShowtimeApiMapper mapper,
                                   BookingService bookingService) {
        this.showtimeService = showtimeService;
        this.mapper = mapper;
        this.bookingService = bookingService;
    }

    @PostMapping
    public ResponseEntity<?> createShowtime(@Valid @RequestBody CreateShowtimeDTO createShowtimeDTO) {
        showtimeService.createShowtime(mapper.toDomain(createShowtimeDTO));
        return ResponseEntity.ok(ApiResponse.success("Tạo suất chiếu thành công"));
    }

    @PutMapping
    public ResponseEntity<?> updateShowtime(@Valid @RequestBody UpdateShowtimeDTO updateShowtimeDTO) {
        showtimeService.updateShowtime(mapper.toDomain(updateShowtimeDTO));
        return ResponseEntity.ok(ApiResponse.success("Cập nhật suất chiếu thành công"));
    }

    @DeleteMapping("/{showtimeId}")
    public ResponseEntity<?> deleteShowtime(@PathVariable Long showtimeId) {
        showtimeService.deleteShowtime(showtimeId);
        return ResponseEntity.ok(ApiResponse.success("Xóa suất chiếu thành công"));
    }

    /**
     * GET /api/v1/admin/showtime/rooms/{roomId}?date=yyyy-MM-dd
     * Xem toàn bộ showtime của phòng theo ngày (bao gồm mọi status: AVAILABLE, SOLD_OUT, HIDDEN, CANCELLED).
     * Dùng để admin xem trước khi thực hiện thao tác.
     */
    @GetMapping("/rooms/{roomId}")
    public ResponseEntity<?> getShowtimesByRoom(
            @PathVariable Long roomId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        List<Showtime> showtimes = showtimeService.getShowtimesByRoomAndDate(roomId, date);
        return ResponseEntity.ok(ApiResponse.success(showtimes.stream()
                .map(mapper::toShowtimeDetailDTO)
                .toList()));
    }

    /**
     * POST /api/v1/admin/showtime/cancel-by-room
     * Batch cancel toàn bộ showtime AVAILABLE của phòng trong khoảng ngày.
     * Tự động refund booking PAID và hủy booking PENDING, gửi email thông báo.
     */
    @PostMapping("/cancel-by-room")
    public ResponseEntity<?> cancelShowtimesByRoom(
            @Valid @RequestBody CancelByRoomRequestDTO requestDTO
    ) {
        List<Showtime> cancelled = showtimeService.cancelShowtimesByRoomBetweenDates(
                requestDTO.getRoomId(),
                requestDTO.getFromDate(),
                requestDTO.getToDate()
        );

        // Xử lý refund cho mỗi showtime đã cancel
        for (Showtime showtime : cancelled) {
            bookingService.refundBookingsForCancelledShowtime(
                    showtime.getShowtimeId(),
                    requestDTO.getReason()
            );
        }

        return ResponseEntity.ok(ApiResponse.success(
                "Đã hủy " + cancelled.size() + " suất chiếu và xử lý refund thành công"));
    }
}
