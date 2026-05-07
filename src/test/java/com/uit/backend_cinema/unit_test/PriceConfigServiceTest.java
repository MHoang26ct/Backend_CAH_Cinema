package com.uit.backend_cinema.unit_test;

import com.uit.backend_cinema.modules.price_config.domain.helper.DayType;
import com.uit.backend_cinema.modules.price_config.domain.helper.MovieFormat;
import com.uit.backend_cinema.modules.price_config.domain.helper.TimeSlot;
import com.uit.backend_cinema.modules.price_config.domain.repository.HolidayRepository;
import com.uit.backend_cinema.modules.price_config.domain.repository.PriceConfigRepository;
import com.uit.backend_cinema.modules.price_config.domain.service.PriceConfigService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PriceConfigServiceTest {

    @InjectMocks
    private PriceConfigService priceConfigService;

    @Mock
    private PriceConfigRepository priceConfigRepository;

    @Mock
    private HolidayRepository holidayRepository;

    @Test
    @DisplayName("Price config đặc biệt: WEEKDAY MORNING + 2D = 0.80 * 1.00")
    void getPriceMultiplierForWeekdayMorning2D() {
        LocalDateTime showtime = LocalDateTime.of(2026, 5, 7, 9, 0);
        when(holidayRepository.isHoliday(showtime.toLocalDate())).thenReturn(false);
        when(priceConfigRepository.findByMovieFormat(MovieFormat.TYPE_2D))
                .thenReturn(Optional.of(new BigDecimal("1.00")));
        when(priceConfigRepository.findByTimeSlot(TimeSlot.MORNING))
                .thenReturn(Optional.of(new BigDecimal("0.80")));

        BigDecimal result = priceConfigService.getPriceMultiplier(showtime, MovieFormat.TYPE_2D);

        assertEquals(0, result.compareTo(new BigDecimal("0.8000")));
    }

    @Test
    @DisplayName("Price config đặc biệt: WEEKDAY AFTERNOON + 3D = 1.00 * 1.30")
    void getPriceMultiplierForWeekdayAfternoon3D() {
        LocalDateTime showtime = LocalDateTime.of(2026, 5, 7, 13, 0);
        when(holidayRepository.isHoliday(showtime.toLocalDate())).thenReturn(false);
        when(priceConfigRepository.findByMovieFormat(MovieFormat.TYPE_3D))
                .thenReturn(Optional.of(new BigDecimal("1.30")));
        when(priceConfigRepository.findByTimeSlot(TimeSlot.AFTERNOON))
                .thenReturn(Optional.of(new BigDecimal("1.00")));

        BigDecimal result = priceConfigService.getPriceMultiplier(showtime, MovieFormat.TYPE_3D);

        assertEquals(0, result.compareTo(new BigDecimal("1.3000")));
    }

    @Test
    @DisplayName("Price config đặc biệt: WEEKDAY EVENING + IMAX = 1.20 * 1.50")
    void getPriceMultiplierForWeekdayEveningImax() {
        LocalDateTime showtime = LocalDateTime.of(2026, 5, 7, 19, 0);
        when(holidayRepository.isHoliday(showtime.toLocalDate())).thenReturn(false);
        when(priceConfigRepository.findByMovieFormat(MovieFormat.TYPE_IMAX))
                .thenReturn(Optional.of(new BigDecimal("1.50")));
        when(priceConfigRepository.findByTimeSlot(TimeSlot.EVENING))
                .thenReturn(Optional.of(new BigDecimal("1.20")));

        BigDecimal result = priceConfigService.getPriceMultiplier(showtime, MovieFormat.TYPE_IMAX);

        assertEquals(0, result.compareTo(new BigDecimal("1.8000")));
    }

    @Test
    @DisplayName("Price config đặc biệt: WEEKEND không cần time_slot, cuối tuần + IMAX = 1.20 * 1.50")
    void getPriceMultiplierForWeekendImax() {
        LocalDateTime weekendDate = LocalDateTime.of(2026, 4, 26, 12, 0);
        when(holidayRepository.isHoliday(weekendDate.toLocalDate())).thenReturn(false);
        when(priceConfigRepository.findByMovieFormat(MovieFormat.TYPE_IMAX))
                .thenReturn(Optional.of(new BigDecimal("1.50")));
        when(priceConfigRepository.findByDayType(DayType.WEEKEND))
                .thenReturn(Optional.of(new BigDecimal("1.20")));

        BigDecimal result = priceConfigService.getPriceMultiplier(weekendDate, MovieFormat.TYPE_IMAX);

        assertEquals(0, result.compareTo(new BigDecimal("1.8000")));
    }

    @Test
    @DisplayName("Price config đặc biệt: HOLIDAY ưu tiên hơn WEEKEND, ngày lễ + 3D = 1.50 * 1.30")
    void getPriceMultiplierForHoliday3D() {
        LocalDateTime holidayDate = LocalDateTime.of(2026, 1, 1, 12, 0);
        when(holidayRepository.isHoliday(holidayDate.toLocalDate())).thenReturn(true);
        when(priceConfigRepository.findByMovieFormat(MovieFormat.TYPE_3D))
                .thenReturn(Optional.of(new BigDecimal("1.30")));
        when(priceConfigRepository.findByDayType(DayType.HOLIDAY))
                .thenReturn(Optional.of(new BigDecimal("1.50")));

        BigDecimal result = priceConfigService.getPriceMultiplier(holidayDate, MovieFormat.TYPE_3D);

        assertEquals(0, result.compareTo(new BigDecimal("1.9500")));
    }

    @Test
    @DisplayName("Price config: dữ liệu đầu vào null thì ném IllegalArgumentException")
    void getPriceMultiplierRejectsNullInputs() {
        assertThrows(IllegalArgumentException.class,
                () -> priceConfigService.getPriceMultiplier(null, MovieFormat.TYPE_2D));
        assertThrows(IllegalArgumentException.class,
                () -> priceConfigService.getPriceMultiplier(LocalDateTime.now(), null));
    }
}
