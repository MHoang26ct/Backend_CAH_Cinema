package com.uit.backend_cinema.modules.seat.api.controller;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.uit.backend_cinema.common.util.ApiResponse;
import com.uit.backend_cinema.modules.seat.api.dto.CreateSeatDTO;
import com.uit.backend_cinema.modules.seat.api.dto.ReplaceSeatMapRequestDTO;
import com.uit.backend_cinema.modules.seat.api.dto.SeatDTO;
import com.uit.backend_cinema.modules.seat.api.mapper.SeatApiMapper;
import com.uit.backend_cinema.modules.seat.domain.entity.Seat;
import com.uit.backend_cinema.modules.seat.domain.service.SeatMapService;
import com.uit.backend_cinema.modules.seat.domain.service.SeatService;

@RestController
@RequestMapping("/api/v1/admin/seats")
public class AdminSeatController {
    private final SeatService seatService;
    private final SeatApiMapper seatApiMapper;
    private final SeatMapService seatMapService;

    public AdminSeatController(SeatService seatService, SeatApiMapper seatApiMapper,
                               SeatMapService seatMapService) {
        this.seatService = seatService;
        this.seatApiMapper = seatApiMapper;
        this.seatMapService = seatMapService;
    }

    @PostMapping("/create")
    public ResponseEntity<?> createSeatMap(
            @NotEmpty(message = "Danh sách ghế không được trống") @Valid @RequestBody
            List<@Valid CreateSeatDTO> createSeatDTOs) {
        List<Seat> seatMap = createSeatDTOs.stream().map(seatApiMapper::toDomain).toList();
        seatService.createSeatMap(seatMap);
        return ResponseEntity.ok(ApiResponse.success("Tạo sơ đồ ghế thành công"));
    }

    @DeleteMapping("/delete/{roomId}")
    public ResponseEntity<?> deleteSeatsByRoomId(@PathVariable Long roomId) {
        seatService.deleteSeatsByRoomId(roomId);
        return ResponseEntity.ok(ApiResponse.success("Xóa sơ đồ ghế thành công"));
    }

    /**
     * PUT /api/v1/admin/seats/replace
     * Thay thế sơ đồ ghế bằng cách tạo phòng mới (Room Cloning).
     * Showtime > 7 ngày tới sẽ tự động migrate sang phòng mới.
     * Showtime ≤ 7 ngày giữ nguyên phòng cũ đến khi chiếu xong.
     */
    @PutMapping("/replace")
    public ResponseEntity<?> replaceSeatMap(
            @Valid @RequestBody ReplaceSeatMapRequestDTO requestDTO
    ) {
        List<Seat> newSeats = requestDTO.getSeats().stream()
                .map(seatApiMapper::toDomain)
                .toList();
        seatMapService.replaceSeatMap(requestDTO.getRoomId(), newSeats);
        return ResponseEntity.ok(ApiResponse.success(
                "Thay thế sơ đồ ghế thành công. Suất chiếu sau 7 ngày đã được chuyển sang phòng mới."));
    }

    @GetMapping("/rooms/{roomId}")
    public ResponseEntity<?> getSeatsByRoomId(@PathVariable Long roomId) {
        List<SeatDTO> seats = seatService.getOriginalSeatsByRoomId(roomId)
                .stream()
                .map(seatApiMapper::toDto)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(seats));
    }
}
