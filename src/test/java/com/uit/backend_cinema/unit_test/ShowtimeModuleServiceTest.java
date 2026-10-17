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
import java.time.Clock;
import java.time.LocalDate;
import com.uit.backend_cinema.modules.booking.domain.repository.BookingRepository;
import java.time.LocalDateTime;
import java.util.List;

import com.uit.backend_cinema.common.exception.BusinessException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
        ShowtimeService showtimeService = new ShowtimeService(showtimeRepository, movieService,
                mock(BookingRepository.class), Clock.systemDefaultZone());
        Showtime showtime = validShowtime();

        when(showtimeRepository.findAllByRoomIdAndDate(eq(1L), any(LocalDate.class))).thenReturn(List.of());
        Movie movie = new Movie();
        movie.setDuration(120);
        doReturn(movie).when(movieService).getById(2L);

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
        showtime.setStatus(ShowtimeStatus.AVAILABLE);
        return showtime;
    }

    @Test
    @DisplayName("validateShowtimeBookable: Thành công khi suất chiếu AVAILABLE và < 7 ngày")
    void validateShowtimeBookable_success() {
        ShowtimeRepository showtimeRepository = mock(ShowtimeRepository.class);
        MovieService movieService = mock(MovieService.class);
        ShowtimeService showtimeService = new ShowtimeService(showtimeRepository, movieService,
                mock(BookingRepository.class), Clock.systemDefaultZone());
        
        Showtime showtime = validShowtime(); // AVAILABLE, tomorrow
        
        // Không quăng lỗi là thành công
        showtimeService.validateShowtimeBookable(showtime);
    }

    @Test
    @DisplayName("validateShowtimeBookable: Ném exception khi trạng thái không phải AVAILABLE")
    void validateShowtimeBookable_failStatus() {
        ShowtimeRepository showtimeRepository = mock(ShowtimeRepository.class);
        MovieService movieService = mock(MovieService.class);
        ShowtimeService showtimeService = new ShowtimeService(showtimeRepository, movieService,
                mock(BookingRepository.class), Clock.systemDefaultZone());
        
        Showtime showtime = validShowtime();
        showtime.setStatus(ShowtimeStatus.CANCELLED);
        
        BusinessException ex = assertThrows(BusinessException.class, () -> showtimeService.validateShowtimeBookable(showtime));
        assertEquals("Suất chiếu không khả dụng để đặt vé", ex.getMessage());
    }

    @Test
    @DisplayName("validateShowtimeBookable: Ném exception khi suất chiếu > 7 ngày")
    void validateShowtimeBookable_failDays() {
        ShowtimeRepository showtimeRepository = mock(ShowtimeRepository.class);
        MovieService movieService = mock(MovieService.class);
        ShowtimeService showtimeService = new ShowtimeService(showtimeRepository, movieService,
                mock(BookingRepository.class), Clock.systemDefaultZone());
        
        Showtime showtime = validShowtime();
        showtime.setStartTime(LocalDateTime.now().plusDays(8));
        
        BusinessException ex = assertThrows(BusinessException.class, () -> showtimeService.validateShowtimeBookable(showtime));
        assertEquals("Chỉ có thể đặt vé cho suất chiếu trong vòng 7 ngày tới", ex.getMessage());
    }

    @Test
    @DisplayName("cancelShowtimesByRoomBetweenDates: Hủy đúng danh sách suất chiếu")
    void cancelShowtimesByRoomBetweenDates_success() {
        ShowtimeRepository showtimeRepository = mock(ShowtimeRepository.class);
        MovieService movieService = mock(MovieService.class);
        ShowtimeService showtimeService = new ShowtimeService(showtimeRepository, movieService,
                mock(BookingRepository.class), Clock.systemDefaultZone());
        
        LocalDate from = LocalDate.now();
        LocalDate to = from.plusDays(2);
        
        Showtime s1 = validShowtime();
        Showtime s2 = validShowtime();
        List<Showtime> activeShowtimes = List.of(s1, s2);
        
        when(showtimeRepository.findActiveByRoomIdBetweenDates(eq(1L), eq(from.atStartOfDay()), eq(to.plusDays(1).atStartOfDay())))
                .thenReturn(activeShowtimes);
                
        List<Showtime> cancelled = showtimeService.cancelShowtimesByRoomBetweenDates(1L, from, to);
        
        assertEquals(2, cancelled.size());
        assertEquals(ShowtimeStatus.CANCELLED, s1.getStatus());
        assertEquals(ShowtimeStatus.CANCELLED, s2.getStatus());
        
        verify(showtimeRepository).save(s1);
        verify(showtimeRepository).save(s2);
    }

    @Test
    @DisplayName("createShowtime: ném lỗi nếu trùng khoảng đệm với suất chiếu đêm hôm trước")
    void createShowtimeOverlapsWithOvernightShowtimeFromPreviousDay() {
        ShowtimeRepository showtimeRepository = mock(ShowtimeRepository.class);
        MovieService movieService = mock(MovieService.class);
        ShowtimeService showtimeService = new ShowtimeService(showtimeRepository, movieService,
                mock(BookingRepository.class), Clock.systemDefaultZone());

        LocalDate today = LocalDate.now().plusDays(2);
        // Suất hôm trước: 23:30 -> 01:30 sáng hôm sau (ngày `today`)
        Showtime overnight = new Showtime();
        overnight.setShowtimeId(99L);
        overnight.setRoomId(1L);
        overnight.setStartTime(today.minusDays(1).atTime(23, 30));
        overnight.setEndTime(today.atTime(1, 30));
        overnight.setStatus(ShowtimeStatus.AVAILABLE);

        when(showtimeRepository.findAllByRoomIdAndDate(eq(1L), eq(today.minusDays(1)))).thenReturn(List.of(overnight));
        when(showtimeRepository.findAllByRoomIdAndDate(eq(1L), eq(today))).thenReturn(List.of());
        when(showtimeRepository.findAllByRoomIdAndDate(eq(1L), eq(today.plusDays(1)))).thenReturn(List.of());

        // Suất mới: bắt đầu lúc 08:00 sáng ngày `today`
        // Nhưng nếu suất mới bắt đầu lúc 01:45 (cách suất cũ chỉ 15 phút < 30 phút buffer)
        // Lưu ý: rule bắt đầu trong khoảng 08:00 - 23:59.
        // Giả sử suất hôm trước kéo dài đến 07:45 sáng (boundary 08:00) và suất mới lúc 08:00 (cách 15 phút < 30 phút)
        overnight.setEndTime(today.atTime(7, 45));

        Showtime newShowtime = validShowtime();
        newShowtime.setStartTime(today.atTime(8, 0));
        newShowtime.setEndTime(today.atTime(10, 0));

        Movie movie = new Movie();
        movie.setDuration(120);
        doReturn(movie).when(movieService).getById(2L);

        BusinessException ex = assertThrows(BusinessException.class, () -> showtimeService.createShowtime(newShowtime));
        assertEquals("Khoảng cách giữa 2 suất chiếu phải ít nhất 30 phút", ex.getMessage());
    }

    @Test
    @DisplayName("createShowtime: bỏ qua các suất chiếu CANCELLED hoặc deleted khi kiểm tra trùng lặp")
    void createShowtimeIgnoresCancelledAndDeletedShowtimes() {
        ShowtimeRepository showtimeRepository = mock(ShowtimeRepository.class);
        MovieService movieService = mock(MovieService.class);
        ShowtimeService showtimeService = new ShowtimeService(showtimeRepository, movieService,
                mock(BookingRepository.class), Clock.systemDefaultZone());

        LocalDate today = LocalDate.now().plusDays(2);
        Showtime cancelledShowtime = new Showtime();
        cancelledShowtime.setShowtimeId(88L);
        cancelledShowtime.setRoomId(1L);
        cancelledShowtime.setStartTime(today.atTime(10, 0));
        cancelledShowtime.setEndTime(today.atTime(12, 0));
        cancelledShowtime.setStatus(ShowtimeStatus.CANCELLED);

        when(showtimeRepository.findAllByRoomIdAndDate(eq(1L), eq(today.minusDays(1)))).thenReturn(List.of());
        when(showtimeRepository.findAllByRoomIdAndDate(eq(1L), eq(today))).thenReturn(List.of(cancelledShowtime));
        when(showtimeRepository.findAllByRoomIdAndDate(eq(1L), eq(today.plusDays(1)))).thenReturn(List.of());

        Showtime newShowtime = validShowtime();
        newShowtime.setStartTime(today.atTime(10, 0));
        newShowtime.setEndTime(today.atTime(12, 0));

        Movie movie = new Movie();
        movie.setDuration(120);
        doReturn(movie).when(movieService).getById(2L);

        showtimeService.createShowtime(newShowtime);

        verify(showtimeRepository).save(newShowtime);
    }
}
