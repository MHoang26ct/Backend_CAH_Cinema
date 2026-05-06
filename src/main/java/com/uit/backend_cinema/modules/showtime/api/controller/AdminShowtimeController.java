package com.uit.backend_cinema.modules.showtime.api.controller;

import com.uit.backend_cinema.common.util.ApiResponse;
import com.uit.backend_cinema.modules.showtime.api.dto.CreateShowtimeDTO;
import com.uit.backend_cinema.modules.showtime.api.dto.UpdateShowtimeDTO;
import com.uit.backend_cinema.modules.showtime.api.mapper.ShowtimeApiMapper;
import com.uit.backend_cinema.modules.showtime.domain.entity.Showtime;
import com.uit.backend_cinema.modules.showtime.domain.service.ShowtimeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/showtime")
public class AdminShowtimeController {
    private final ShowtimeService showtimeService;
    private final ShowtimeApiMapper mapper;

    public AdminShowtimeController(ShowtimeService showtimeService, ShowtimeApiMapper mapper) {
        this.showtimeService = showtimeService;
        this.mapper = mapper;
    }

    @PostMapping
    public ResponseEntity<?> createShowtime(@RequestBody CreateShowtimeDTO createShowtimeDTO) {
        showtimeService.createShowtime(mapper.toDomain(createShowtimeDTO));
        return ResponseEntity.ok(ApiResponse.success("Tạo suất chiếu thành công"));
    }

    @PutMapping
    public ResponseEntity<?> updateShowtime(@RequestBody UpdateShowtimeDTO updateShowtimeDTO) {
        showtimeService.updateShowtime(mapper.toDomain(updateShowtimeDTO));
        return ResponseEntity.ok(ApiResponse.success("Cập nhật suất chiếu thành công"));
    }

    @DeleteMapping("/{showtimeId}")
    public ResponseEntity<?> deleteShowtime(@PathVariable Long showtimeId) {
        showtimeService.deleteShowtime(showtimeId);
        return ResponseEntity.ok(ApiResponse.success("Xóa suất chiếu thành công"));
    }
}
