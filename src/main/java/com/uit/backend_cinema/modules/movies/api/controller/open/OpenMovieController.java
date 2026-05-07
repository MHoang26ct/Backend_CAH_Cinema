package com.uit.backend_cinema.modules.movies.api.controller.open;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.uit.backend_cinema.common.util.ApiResponse;
import com.uit.backend_cinema.modules.movies.api.dto.MovieDetailDTO;
import com.uit.backend_cinema.modules.movies.api.dto.MovieSummaryDTO;
import com.uit.backend_cinema.modules.movies.api.mapper.MovieApiMapper;
import com.uit.backend_cinema.modules.movies.domain.service.MovieService;

@RestController
@RequestMapping("/api/v1/public/movies")
public class OpenMovieController {
    private final MovieService movieService;
    private final MovieApiMapper movieApiMapper;

    public OpenMovieController(MovieService movieService, MovieApiMapper movieApiMapper) {
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
