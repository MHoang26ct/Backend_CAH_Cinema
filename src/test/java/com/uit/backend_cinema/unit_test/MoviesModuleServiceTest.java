package com.uit.backend_cinema.unit_test;

import com.uit.backend_cinema.common.exception.BusinessException;
import com.uit.backend_cinema.modules.movies.domain.entity.Movie;
import com.uit.backend_cinema.modules.movies.domain.repository.MovieRepository;
import com.uit.backend_cinema.modules.movies.domain.service.MovieService;
import com.uit.backend_cinema.modules.showtime.domain.repository.ShowtimeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MoviesModuleServiceTest {

    @Test
    @DisplayName("Movie module: không tạo phim trùng title và release date")
    void createMovieRejectsDuplicateMovie() {
        MovieRepository movieRepository = mock(MovieRepository.class);
        ShowtimeRepository showtimeRepository = mock(ShowtimeRepository.class);
        MovieService movieService = new MovieService(movieRepository, showtimeRepository);
        Movie movie = new Movie();
        movie.setTitle("CAH Cinema");
        movie.setReleaseDate(LocalDate.of(2026, 5, 7));

        when(movieRepository.isDuplicate("CAH Cinema", LocalDate.of(2026, 5, 7))).thenReturn(true);

        assertThrows(BusinessException.class, () -> movieService.createMovie(movie));
        verify(movieRepository, never()).save(any(Movie.class));
    }
}
