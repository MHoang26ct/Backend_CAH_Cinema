package com.uit.backend_cinema.unit_test;

import com.uit.backend_cinema.common.exception.BusinessException;
import com.uit.backend_cinema.common.exception.ErrorCode;
import com.uit.backend_cinema.modules.seat.domain.entity.Seat;
import com.uit.backend_cinema.modules.seat.domain.entity.SeatType;
import com.uit.backend_cinema.modules.seat.domain.repository.SeatLockRepository;
import com.uit.backend_cinema.modules.seat.domain.repository.SeatRepository;
import com.uit.backend_cinema.modules.seat.domain.repository.SeatTypeRepository;
import com.uit.backend_cinema.modules.seat.domain.service.SeatService;
import com.uit.backend_cinema.modules.ticket.domain.service.TicketService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeatCoupleValidationTest {

    @InjectMocks
    private SeatService seatService;

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private SeatLockRepository seatLockRepository;

    @Mock
    private SeatTypeRepository seatTypeRepository;

    @Mock
    private TicketService ticketService;

    private Seat createSeat(Long id, BigDecimal row, BigDecimal col, String typeName) {
        Seat seat = new Seat();
        seat.setSeatId(id);
        seat.setRoomId(1L);
        seat.setSeatRow(row);
        seat.setSeatCol(col);

        SeatType type = new SeatType();
        type.setSeatTypeId(typeName.hashCode() * 1L);
        type.setTypeName(typeName);
        seat.setSeatType(type);

        return seat;
    }

    @Test
    @DisplayName("1. Chọn ghế thường/VIP (không phải couple) -> Hợp lệ")
    void testStandardSeatsOnly() {
        Seat seat1 = createSeat(1L, BigDecimal.ONE, BigDecimal.ONE, "STANDARD");
        Seat seat2 = createSeat(2L, BigDecimal.ONE, BigDecimal.valueOf(2), "VIP");

        when(seatRepository.findByIds(Arrays.asList(1L, 2L))).thenReturn(Arrays.asList(seat1, seat2));

        List<Seat> result = seatService.getValidatedSeatsForBooking(Arrays.asList(1L, 2L), 1L);
        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("2. Chọn cặp ghế đôi (COUPLE) đi liền nhau -> Hợp lệ")
    void testCoupleSeatsPair() {
        Seat seat1 = createSeat(1L, BigDecimal.ONE, BigDecimal.valueOf(3), "COUPLE");
        Seat seat2 = createSeat(2L, BigDecimal.ONE, BigDecimal.valueOf(4), "COUPLE");

        when(seatRepository.findByIds(Arrays.asList(1L, 2L))).thenReturn(Arrays.asList(seat1, seat2));

        List<Seat> result = seatService.getValidatedSeatsForBooking(Arrays.asList(1L, 2L), 1L);
        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("3. Chọn 1 ghế đôi (COUPLE) đơn lẻ -> Lỗi chưa đủ cặp")
    void testSingleCoupleSeat() {
        Seat seat1 = createSeat(1L, BigDecimal.ONE, BigDecimal.valueOf(3), "COUPLE");

        when(seatRepository.findByIds(Collections.singletonList(1L))).thenReturn(Collections.singletonList(seat1));

        BusinessException ex = assertThrows(BusinessException.class, () -> {
            seatService.getValidatedSeatsForBooking(Collections.singletonList(1L), 1L);
        });
        assertEquals(ErrorCode.VALIDATION_FAILED, ex.getCode());
        assertTrue(ex.getMessage().contains("chưa đủ cặp"));
    }

    @Test
    @DisplayName("4. Chọn 2 ghế đôi (COUPLE) ở 2 hàng khác nhau -> Lỗi chưa đủ cặp")
    void testCoupleSeatsDifferentRows() {
        Seat seat1 = createSeat(1L, BigDecimal.ONE, BigDecimal.valueOf(3), "COUPLE");
        Seat seat2 = createSeat(2L, BigDecimal.valueOf(2), BigDecimal.valueOf(4), "COUPLE");

        when(seatRepository.findByIds(Arrays.asList(1L, 2L))).thenReturn(Arrays.asList(seat1, seat2));

        BusinessException ex = assertThrows(BusinessException.class, () -> {
            seatService.getValidatedSeatsForBooking(Arrays.asList(1L, 2L), 1L);
        });
        assertEquals(ErrorCode.VALIDATION_FAILED, ex.getCode());
        assertTrue(ex.getMessage().contains("chưa đủ cặp"));
    }

    @Test
    @DisplayName("5. Hỗn hợp: Cặp ghế đôi hợp lệ + Ghế thường -> Hợp lệ")
    void testMixedSeatsValid() {
        Seat seat1 = createSeat(1L, BigDecimal.ONE, BigDecimal.valueOf(3), "COUPLE");
        Seat seat2 = createSeat(2L, BigDecimal.ONE, BigDecimal.valueOf(4), "COUPLE");
        Seat seat3 = createSeat(3L, BigDecimal.valueOf(2), BigDecimal.valueOf(1), "STANDARD");

        when(seatRepository.findByIds(Arrays.asList(1L, 2L, 3L))).thenReturn(Arrays.asList(seat1, seat2, seat3));

        List<Seat> result = seatService.getValidatedSeatsForBooking(Arrays.asList(1L, 2L, 3L), 1L);
        assertEquals(3, result.size());
    }
}
