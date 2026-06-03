package com.uit.backend_cinema.unit_test;

import com.uit.backend_cinema.modules.seat.domain.entity.Seat;
import com.uit.backend_cinema.modules.seat.domain.entity.SeatType;
import com.uit.backend_cinema.modules.seat.domain.repository.SeatLockRepository;
import com.uit.backend_cinema.modules.seat.domain.repository.SeatRepository;
import com.uit.backend_cinema.modules.seat.domain.repository.SeatTypeRepository;
import com.uit.backend_cinema.modules.seat.domain.service.SeatService;
import com.uit.backend_cinema.modules.ticket.domain.service.TicketService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SeatModuleServiceTest {

    @Test
    @DisplayName("Seat module: nếu lock ghế thứ hai thất bại thì rollback ghế đã lock")
    void preLockSeatsRollsBackPreviouslyLockedSeatsOnFailure() {
        SeatRepository seatRepository = mock(SeatRepository.class);
        SeatLockRepository seatLockRepository = mock(SeatLockRepository.class);
        SeatTypeRepository seatTypeRepository = mock(SeatTypeRepository.class);
        TicketService ticketService = mock(TicketService.class);
        SeatService seatService = new SeatService(seatRepository, seatLockRepository, seatTypeRepository, ticketService);
        Seat seat1 = seat(1L, 100L);
        Seat seat2 = seat(2L, 100L);

        when(seatRepository.findByIds(List.of(1L, 2L))).thenReturn(List.of(seat1, seat2));
        when(seatLockRepository.lockSeat(7L, 1L, 50L, 300L)).thenReturn(true);
        when(seatLockRepository.lockSeat(7L, 2L, 50L, 300L)).thenReturn(false);

        assertFalse(seatService.preLockSeats(7L, List.of(1L, 2L), 100L, 50L));
        verify(seatLockRepository).unlock(7L, 1L);
    }

    @Test
    @DisplayName("Seat module: Lấy danh sách sơ đồ ghế gốc theo roomId")
    void shouldReturnOriginalSeatsByRoomId() {
        SeatRepository seatRepository = mock(SeatRepository.class);
        SeatLockRepository seatLockRepository = mock(SeatLockRepository.class);
        SeatTypeRepository seatTypeRepository = mock(SeatTypeRepository.class);
        TicketService ticketService = mock(TicketService.class);
        SeatService seatService = new SeatService(seatRepository, seatLockRepository, seatTypeRepository, ticketService);

        Long roomId = 100L;
        Seat seat1 = seat(1L, roomId);
        Seat seat2 = seat(2L, roomId);
        List<Seat> mockSeats = List.of(seat1, seat2);

        when(seatRepository.findByRoomId(roomId)).thenReturn(mockSeats);

        List<Seat> result = seatService.getOriginalSeatsByRoomId(roomId);

        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getSeatId());
        assertEquals(2L, result.get(1).getSeatId());
        verify(seatRepository).findByRoomId(roomId);
    }

    private Seat seat(Long seatId, Long roomId) {
        SeatType seatType = new SeatType();
        seatType.setTypeName("STANDARD");
        Seat seat = new Seat();
        seat.setSeatId(seatId);
        seat.setRoomId(roomId);
        seat.setSeatType(seatType);
        seat.setSeatRow(BigDecimal.ONE);
        seat.setSeatCol(BigDecimal.valueOf(seatId));
        return seat;
    }
}
