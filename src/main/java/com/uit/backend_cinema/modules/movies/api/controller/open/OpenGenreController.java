package com.uit.backend_cinema.modules.movies.api.controller.open;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.uit.backend_cinema.common.util.ApiResponse;
import com.uit.backend_cinema.modules.movies.api.dto.GenreDTO;
import com.uit.backend_cinema.modules.movies.api.mapper.GenreApiMapper;
import com.uit.backend_cinema.modules.movies.domain.service.GenreService;

@RestController
@RequestMapping("/api/v1/public/genres")
public class OpenGenreController {
    private final GenreService genreService;
    private final GenreApiMapper mapper;

    public OpenGenreController(GenreService genreService, GenreApiMapper mapper) {
        this.mapper = mapper;
        this.genreService = genreService;
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<GenreDTO>>> findAll() {
        List<GenreDTO> genres = genreService.findAll().stream().map(mapper::toDTO).toList();
        return ResponseEntity.ok(ApiResponse.success(genres, "Lấy danh sách thể loại phim thành công"));
    }
}
