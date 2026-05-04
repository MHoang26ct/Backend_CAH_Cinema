package com.uit.backend_cinema.modules.cinema.api.controller;

import com.uit.backend_cinema.common.util.ApiResponse;
import com.uit.backend_cinema.modules.cinema.api.entity.CinemaResponseDTO;
import com.uit.backend_cinema.modules.cinema.api.entity.CreateCinemaRequestDTO;
import com.uit.backend_cinema.modules.cinema.api.entity.UpdateCinemaRequestDTO;
import com.uit.backend_cinema.modules.cinema.api.entity.RoomResponseDTO;
import com.uit.backend_cinema.modules.cinema.api.entity.CreateRoomRequestDTO;
import com.uit.backend_cinema.modules.cinema.api.entity.UpdateRoomRequestDTO;
import com.uit.backend_cinema.modules.cinema.api.mapper.CinemaApiMapper;
import com.uit.backend_cinema.modules.cinema.domain.service.CinemaService;
import com.uit.backend_cinema.modules.cinema.domain.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/cinemas")
public class CinemaAdminController {

    private final CinemaService cinemaService;
    private final RoomService roomService;
    private final CinemaApiMapper mapper;

    public CinemaAdminController(CinemaService cinemaService, RoomService roomService, CinemaApiMapper mapper) {
        this.cinemaService = cinemaService;
        this.roomService = roomService;
        this.mapper = mapper;
    }

    // ───── Cinema endpoints ─────

    @GetMapping("/{cinemaId}")
    public ResponseEntity<?> getCinemaById(@PathVariable Long cinemaId) {
        CinemaResponseDTO response = mapper.toDTO(cinemaService.findById(cinemaId));
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy thông tin rạp thành công"));
    }

    @PostMapping
    public ResponseEntity<?> createCinema(@Valid @RequestBody CreateCinemaRequestDTO request) {
        CinemaResponseDTO response = mapper.toDTO(cinemaService.create(mapper.toDomain(request)));
        return ResponseEntity.ok(ApiResponse.success(response, "Tạo rạp thành công"));
    }

    @PutMapping("/{cinemaId}")
    public ResponseEntity<?> updateCinema(@PathVariable Long cinemaId,
                                          @Valid @RequestBody UpdateCinemaRequestDTO request) {
        request.setCinemaId(cinemaId);
        CinemaResponseDTO response = mapper.toDTO(cinemaService.update(mapper.toDomain(request)));
        return ResponseEntity.ok(ApiResponse.success(response, "Cập nhật rạp thành công"));
    }

    @DeleteMapping("/{cinemaId}")
    public ResponseEntity<?> deleteCinema(@PathVariable Long cinemaId) {
        cinemaService.delete(cinemaId);
        return ResponseEntity.ok(ApiResponse.success(null, "Xóa rạp thành công"));
    }

    // ───── Room endpoints (nested under cinema) ─────

    @GetMapping("/{cinemaId}/rooms")
    public ResponseEntity<?> getRoomsByCinema(@PathVariable Long cinemaId) {
        List<RoomResponseDTO> response = roomService.findAllByCinemaId(cinemaId).stream()
                .map(mapper::toDTO)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy danh sách phòng chiếu thành công"));
    }

    @PostMapping("/{cinemaId}/rooms")
    public ResponseEntity<?> createRoom(@PathVariable Long cinemaId,
                                        @Valid @RequestBody CreateRoomRequestDTO request) {
        request.setCinemaId(cinemaId);
        RoomResponseDTO response = mapper.toDTO(roomService.create(mapper.toDomain(request)));
        return ResponseEntity.ok(ApiResponse.success(response, "Tạo phòng chiếu thành công"));
    }

    @PutMapping("/rooms/{roomId}")
    public ResponseEntity<?> updateRoom(@PathVariable Long roomId,
                                        @Valid @RequestBody UpdateRoomRequestDTO request) {
        request.setRoomId(roomId);
        RoomResponseDTO response = mapper.toDTO(roomService.update(mapper.toDomain(request)));
        return ResponseEntity.ok(ApiResponse.success(response, "Cập nhật phòng chiếu thành công"));
    }

    @DeleteMapping("/rooms/{roomId}")
    public ResponseEntity<?> deleteRoom(@PathVariable Long roomId) {
        roomService.delete(roomId);
        return ResponseEntity.ok(ApiResponse.success(null, "Xóa phòng chiếu thành công"));
    }
}
