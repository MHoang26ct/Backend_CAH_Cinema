package com.uit.backend_cinema.unit_test;

import com.uit.backend_cinema.modules.price_config.domain.entity.Holiday;
import com.uit.backend_cinema.modules.price_config.domain.repository.HolidayRepository;
import com.uit.backend_cinema.modules.price_config.domain.service.HolidayService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HolidayServiceTest {
    private final HolidayRepository repository = mock(HolidayRepository.class);
    private final HolidayService service = new HolidayService(repository);

    private Holiday holiday() {
        Holiday holiday = new Holiday();
        holiday.setName("Quốc khánh");
        holiday.setDate(LocalDate.of(2026, 9, 2));
        holiday.setIsRecurring(true);
        return holiday;
    }

    @Test
    @DisplayName("HOL01: Ngày lễ mới được lưu và trả về kết quả repository")
    void createsNewHoliday() {
        Holiday input = holiday();
        Holiday saved = holiday();
        saved.setHolidayId(7L);
        when(repository.isHoliday(input.getDate())).thenReturn(false);
        when(repository.save(input)).thenReturn(saved);

        assertSame(saved, service.createHoliday(input));
        verify(repository).isHoliday(input.getDate());
        verify(repository).save(input);
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("HOL02: Ngày lễ trùng gây IllegalArgumentException và không lưu")
    void rejectsDuplicateHoliday() {
        Holiday input = holiday();
        when(repository.isHoliday(input.getDate())).thenReturn(true);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.createHoliday(input));
        assertEquals("Ngày lễ đã tồn tại", exception.getMessage());
        verify(repository).isHoliday(input.getDate());
        verify(repository, never()).save(any());
        verifyNoMoreInteractions(repository);
    }
}
