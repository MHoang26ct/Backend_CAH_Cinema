package com.uit.backend_cinema.modules.movies.api.controller.open;

import org.springdoc.core.annotations.ParameterObject;
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
import com.uit.backend_cinema.modules.movies.domain.service.MovieService.FeaturedMoviesResult;

import java.util.List;

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
    // GET
    // /api/v1/user/movies/public?title=avenger&genreId=1&ageRating=T13&page=0&size=10
    @GetMapping
    public ResponseEntity<?> search(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Long genreId,
            @RequestParam(required = false) String ageRating,
            @ParameterObject @PageableDefault(size = 10, sort = "releaseDate", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
        Page<MovieSummaryDTO> result = movieService.search(title, genreId, ageRating, pageable)
                .map(movieApiMapper::toSummaryDto);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // 5 phim đang chiếu + 5 phim sắp chiếu (cho màn hình Home)
    // GET /api/v1/public/movies/featured
    @GetMapping("/featured")
    public ResponseEntity<?> getFeatured() {
        FeaturedMoviesResult featured = movieService.getFeaturedMovies();
        record FeaturedResponse(List<MovieSummaryDTO> nowShowing, List<MovieSummaryDTO> upcoming) {
        }
        FeaturedResponse response = new FeaturedResponse(
                featured.nowShowing().stream().map(movieApiMapper::toSummaryDto).toList(),
                featured.upcoming().stream().map(movieApiMapper::toSummaryDto).toList());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // Chi tiết 1 phim
    // GET /api/v1/user/movies/public/1
    @GetMapping("/{id}")
    public ResponseEntity<?> getDetail(@PathVariable Long id) {
        MovieDetailDTO result = movieApiMapper.toDetailDto(movieService.getById(id));
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
