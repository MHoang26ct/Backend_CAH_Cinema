package com.uit.backend_cinema.unit_test;

import com.uit.backend_cinema.common.exception.BusinessException;
import com.uit.backend_cinema.modules.report.api.mapper.ReportApiMapper;
import com.uit.backend_cinema.modules.report.domain.repository.ReportRepository;
import com.uit.backend_cinema.modules.report.domain.service.ReportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private ReportApiMapper reportApiMapper;

    @InjectMocks
    private ReportService reportService;

    @Test
    @DisplayName("Lỗi khi 'from' lớn hơn 'to'")
    void getOverview_throwsWhenFromAfterTo() {
        LocalDate from = LocalDate.of(2025, 2, 1);
        LocalDate to   = LocalDate.of(2025, 1, 31);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> reportService.getOverview(from, to));

        assertEquals("Ngày bắt đầu (from) không được lớn hơn ngày kết thúc (to)", ex.getMessage());
    }

    @Test
    @DisplayName("Lỗi khi khoảng cách vượt quá giới hạn MAX_DAYS (366 ngày)")
    void getOverview_throwsWhenDateRangeTooLarge() {
        LocalDate from = LocalDate.of(2023, 1, 1);
        LocalDate to   = LocalDate.of(2024, 1, 5);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> reportService.getOverview(from, to));

        assertTrue(ex.getMessage().contains("Khoảng thời gian truy vấn tối đa là 366 ngày"));
    }

    @Test
    @DisplayName("Lỗi khi 'from' hoặc 'to' bị null")
    void getOverview_throwsWhenDateIsNull() {
        BusinessException ex1 = assertThrows(BusinessException.class,
                () -> reportService.getOverview(null, LocalDate.now()));
        assertEquals("Tham số 'from' và 'to' là bắt buộc", ex1.getMessage());

        BusinessException ex2 = assertThrows(BusinessException.class,
                () -> reportService.getOverview(LocalDate.now(), null));
        assertEquals("Tham số 'from' và 'to' là bắt buộc", ex2.getMessage());
    }
}
