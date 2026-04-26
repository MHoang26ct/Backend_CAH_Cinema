package com.uit.backend_cinema.unit_test;

import com.uit.backend_cinema.modules.price_config.domain.helper.DayType;
import com.uit.backend_cinema.modules.price_config.domain.helper.MovieFormat;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PriceConfigServiceTest {

    @InjectMocks
    private PriceConfigService priceConfigService;

    @Mock
    private PriceConfigRepository priceConfigRepository;

    @Mock
    private HolidayRepository holidayRepository;

    @Test
    @DisplayName("Nên trả về 1.95 nếu là ngày lễ + 3D")
    public void testGetPriceMultiplierWithHoliday() {
        LocalDateTime holidayDate = LocalDateTime.of(2026, 1, 1, 12, 0);
        MovieFormat format3D = MovieFormat.TYPE_3D;

        // Gia lap: khi goi vao db lay he so cho dinh dang 3d thi tra ve 1.3
        when(priceConfigRepository.findByMovieFormat(format3D))
                .thenReturn(Optional.of(new BigDecimal("1.3")));

        // Gia lap: day la ngay le
        when(holidayRepository.isHoliday(holidayDate.toLocalDate()))
                .thenReturn(true);

        // Gia lap: He so ngay le la 1.5
        when(priceConfigRepository.findByDayType(DayType.HOLIDAY))
                .thenReturn(Optional.of(new BigDecimal("1.5")));

        BigDecimal result = priceConfigService.getPriceMultiplier(holidayDate, format3D);

        assertTrue(result.compareTo(BigDecimal.valueOf(1.95)) == 0);
    }

    @Test
    @DisplayName("Nên trả về 1.8 nếu là cuối tuần + IMAX")
    public void testGetPriceMultiplierWithWeekend() {
        LocalDateTime weekendDate = LocalDateTime.of(2026, 4, 26, 12, 0);
        MovieFormat formatIMAX = MovieFormat.TYPE_IMAX;

        // Gia lap: IMAX -> 1.5
        when(priceConfigRepository.findByMovieFormat(formatIMAX))
                .thenReturn(Optional.of(new BigDecimal("1.5")));

        // Gia lap: Cuoi tuan -> 1.2
        when(priceConfigRepository.findByDayType(DayType.WEEKEND))
                .thenReturn(Optional.of(new BigDecimal("1.2")));

        // Gia lap: Ngay cuoi tuan + IMAX -> 1.8
        BigDecimal result = priceConfigService.getPriceMultiplier(weekendDate, formatIMAX);

        assertTrue(result.compareTo(BigDecimal.valueOf(1.8)) == 0);
    }
}
