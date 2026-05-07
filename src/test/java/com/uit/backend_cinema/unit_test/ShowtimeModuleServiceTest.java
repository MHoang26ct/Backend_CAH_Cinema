package com.uit.backend_cinema.unit_test;

import com.uit.backend_cinema.modules.movies.domain.entity.Movie;
import com.uit.backend_cinema.modules.movies.domain.service.MovieService;
import com.uit.backend_cinema.modules.price_config.domain.helper.MovieFormat;
import com.uit.backend_cinema.modules.showtime.domain.entity.Showtime;
import com.uit.backend_cinema.modules.showtime.domain.entity.ShowtimeStatus;
import com.uit.backend_cinema.modules.showtime.domain.repository.ShowtimeRepository;
import com.uit.backend_cinema.modules.showtime.domain.service.ShowtimeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShowtimeModuleServiceTest {

    @Test
    @DisplayName("Showtime module: suất chiếu hợp lệ được set trạng thái mặc định AVAILABLE")
    void createShowtimeSetsDefaultStatus() {
        ShowtimeRepository showtimeRepository = mock(ShowtimeRepository.class);
        MovieService movieService = mock(MovieService.class);
        ShowtimeService showtimeService = new ShowtimeService(showtimeRepository, movieService);
        Showtime showtime = validShowtime();

        when(showtimeRepository.findAllByRoomIdAndDate(eq(1L), any(LocalDate.class))).thenReturn(List.of());
        doReturn(new Movie()).when(movieService).getById(2L);

        showtimeService.createShowtime(showtime);

        assertEquals(ShowtimeStatus.AVAILABLE, showtime.getStatus());
        assertEquals(false, showtime.getIsDeleted());
        verify(showtimeRepository).save(showtime);
    }

    private Showtime validShowtime() {
        Showtime showtime = new Showtime();
        showtime.setMovieId(2L);
        showtime.setRoomId(1L);
        showtime.setFormat(MovieFormat.TYPE_2D);
        showtime.setBasePrice(BigDecimal.valueOf(100_000));
        showtime.setStartTime(LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0));
        showtime.setEndTime(showtime.getStartTime().plusHours(2));
        return showtime;
    }
}
