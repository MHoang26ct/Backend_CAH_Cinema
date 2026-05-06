package com.uit.backend_cinema.modules.seat.api.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.uit.backend_cinema.modules.seat.api.mapper.SeatApiMapper;
import com.uit.backend_cinema.common.util.ApiResponse;
import com.uit.backend_cinema.modules.seat.api.dto.CreateSeatDTO;
import com.uit.backend_cinema.modules.seat.domain.entity.Seat;
import com.uit.backend_cinema.modules.seat.domain.service.SeatService;

import io.swagger.v3.oas.annotations.parameters.RequestBody;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/seats")
public class AdminSeatController {
    private final SeatService seatService;
    private final SeatApiMapper seatApiMapper;

    public AdminSeatController(SeatService seatService, SeatApiMapper seatApiMapper) {
        this.seatService = seatService;
        this.seatApiMapper = seatApiMapper;
    }

    @PostMapping("/create")
    public ResponseEntity<?> createSeatMap(
            @Valid @RequestBody List<CreateSeatDTO> createSeatDTOs) {
        List<Seat> seatMap = createSeatDTOs.stream().map(seatApiMapper::toDomain).toList();
        seatService.createSeatMap(seatMap);
        return ResponseEntity.ok(ApiResponse.success("Tạo sơ đồ ghế thành công"));
    }

    @DeleteMapping("/delete/{roomId}")
    public ResponseEntity<?> deleteSeatsByRoomId(@PathVariable Long roomId) {
        seatService.deleteSeatsByRoomId(roomId);
        return ResponseEntity.ok(ApiResponse.success("Xóa sơ đồ ghế thành công"));
    }
}
