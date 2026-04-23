package com.uit.backend_cinema.modules.user.movies.api.controller;

import com.uit.backend_cinema.common.util.ApiResponse;
import com.uit.backend_cinema.modules.user.movies.api.dto.MovieDetailDTO;
import com.uit.backend_cinema.modules.user.movies.api.dto.MovieSummaryDTO;
import com.uit.backend_cinema.modules.user.movies.api.mapper.MovieApiMapper;
import com.uit.backend_cinema.modules.user.movies.domain.service.MovieService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/user/movies/public")
public class MovieController {
    private final MovieService movieService;
    private final MovieApiMapper movieApiMapper;

    public MovieController(MovieService movieService, MovieApiMapper movieApiMapper) {
        this.movieService = movieService;
        this.movieApiMapper = movieApiMapper;
    }

    // Danh sách + tìm kiếm
    // GET /api/v1/user/movies/public?title=avenger&genreId=1&ageRating=T13&page=0&size=10
    @GetMapping
    public ResponseEntity<?> search(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Long genreId,
            @RequestParam(required = false) String ageRating,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        Page<MovieSummaryDTO> result = movieService.search(title, genreId, ageRating, pageable)
                .map(movieApiMapper::toSummaryDto);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // Chi tiết 1 phim
    // GET /api/v1/user/movies/public/1
    @GetMapping("/{id}")
    public ResponseEntity<?> getDetail(@PathVariable Long id) {
        MovieDetailDTO result = movieApiMapper.toDetailDto(movieService.getById(id));
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
