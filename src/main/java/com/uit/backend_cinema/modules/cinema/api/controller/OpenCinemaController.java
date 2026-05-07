package com.uit.backend_cinema.modules.cinema.api.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.uit.backend_cinema.common.util.ApiResponse;
import com.uit.backend_cinema.modules.cinema.api.entity.CinemaResponseDTO;
import com.uit.backend_cinema.modules.cinema.api.mapper.CinemaApiMapper;
import com.uit.backend_cinema.modules.cinema.domain.service.CinemaService;

@RestController
@RequestMapping("/api/v1/public/cinemas")
public class OpenCinemaController {

    private final CinemaService cinemaService;
    private final CinemaApiMapper mapper;

    public OpenCinemaController(CinemaService cinemaService, CinemaApiMapper mapper) {
        this.cinemaService = cinemaService;
        this.mapper = mapper;
    }

    @GetMapping
    public ResponseEntity<?> getAllCinemas() {
        List<CinemaResponseDTO> response = cinemaService.findAll().stream()
                .map(mapper::toDTO)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy danh sách rạp thành công"));
    }
}
