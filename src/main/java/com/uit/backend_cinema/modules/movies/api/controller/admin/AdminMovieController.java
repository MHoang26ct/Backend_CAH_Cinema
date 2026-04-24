package com.uit.backend_cinema.modules.movies.api.controller.admin;

import com.uit.backend_cinema.common.util.ApiResponse;
import com.uit.backend_cinema.modules.movies.api.dto.MovieDetailDTO;
import com.uit.backend_cinema.modules.movies.api.dto.UpdateOrCreateMovieDTO;
import com.uit.backend_cinema.modules.movies.api.mapper.MovieApiMapper;
import com.uit.backend_cinema.modules.movies.domain.entity.Movie;
import com.uit.backend_cinema.modules.movies.domain.service.MovieService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/movies")
public class AdminMovieController {
    private final MovieService movieService;
    private final MovieApiMapper mapper;

    public AdminMovieController(MovieService movieService, MovieApiMapper mapper) {
        this.mapper = mapper;
        this.movieService = movieService;
    }

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<MovieDetailDTO>> createMovie(
            @Valid @RequestBody UpdateOrCreateMovieDTO requestDTO) {
        Movie newMovie = mapper.toDomain(requestDTO);
        MovieDetailDTO movie = mapper.toDetailDto(movieService.createMovie(newMovie));
        return ResponseEntity.ok(ApiResponse.success(movie, "Tạo phim thành công"));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<ApiResponse<MovieDetailDTO>> updateMovie(@PathVariable Long id,
            @Valid @RequestBody UpdateOrCreateMovieDTO requestDTO) {
        Movie updatedMovie = mapper.toDomain(requestDTO);
        MovieDetailDTO movie = mapper.toDetailDto(movieService.updateMovie(id, updatedMovie));
        return ResponseEntity.ok(ApiResponse.success(movie, "Cập nhật phim thành công"));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ApiResponse<MovieDetailDTO>> deleteMovie(@PathVariable Long id) {
        MovieDetailDTO movie = mapper.toDetailDto(movieService.deleteMovie(id));
        return ResponseEntity.ok(ApiResponse.success(movie, "Xóa phim thành công"));
    }
}
