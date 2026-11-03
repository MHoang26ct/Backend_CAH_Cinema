package com.uit.backend_cinema.unit_test;

import com.uit.backend_cinema.common.exception.BusinessException;
import com.uit.backend_cinema.common.exception.ErrorCode;
import com.uit.backend_cinema.modules.seat.domain.entity.Seat;
import com.uit.backend_cinema.modules.seat.domain.entity.SeatType;
import com.uit.backend_cinema.modules.seat.domain.repository.SeatLockRepository;
import com.uit.backend_cinema.modules.seat.domain.repository.SeatRepository;
import com.uit.backend_cinema.modules.seat.domain.repository.SeatTypeRepository;
import com.uit.backend_cinema.modules.seat.domain.service.SeatService;
import com.uit.backend_cinema.modules.seat.infrastructure.persistence.RedisSeatLockRepositoryImpl;
import com.uit.backend_cinema.modules.ticket.domain.service.TicketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Unit contracts only: no Redis server, elapsed-time expiry, or concurrent ownership proof. */
class ScopeSeatDesignTest {
    private final SeatRepository seats = mock(SeatRepository.class);
    private final SeatLockRepository locks = mock(SeatLockRepository.class);
    private final TicketService tickets = mock(TicketService.class);
    private SeatService service;

    @BeforeEach
    void setUp() {
        service = new SeatService(seats, locks, mock(SeatTypeRepository.class), tickets);
    }

    @Test
    @DisplayName("UT01: pre-lock requests 300 seconds from mock; not actual Redis elapsed-time proof")
    void ut01PreLockRequests300SecondsForEverySeat() {
        when(seats.findByIds(List.of(1L, 2L))).thenReturn(List.of(seat(1L, 100L, "STANDARD"),
                seat(2L, 100L, "STANDARD")));
        when(locks.lockSeat(7L, 1L, 50L, 300L)).thenReturn(true);
        when(locks.lockSeat(7L, 2L, 50L, 300L)).thenReturn(true);

        assertTrue(service.preLockSeats(7L, List.of(1L, 2L), 100L, 50L));

        var order = inOrder(tickets, locks);
        order.verify(tickets).validateSeatsNotSold(7L, List.of(1L, 2L));
        order.verify(locks).lockSeat(7L, 1L, 50L, 300L);
        order.verify(locks).lockSeat(7L, 2L, 50L, 300L);
        verifyNoMoreInteractions(locks);
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("UT01: Redis adapter sends SET NX with 300 SECONDS to mock, not an expiry integration test")
    void ut01RedisAdapterRequests300SecondsWithoutMeasuringExpiry() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.setIfAbsent("lock:showtime:7:seat:1", "50", 300L, TimeUnit.SECONDS)).thenReturn(true);
        RedisSeatLockRepositoryImpl repository = new RedisSeatLockRepositoryImpl(redis);

        assertTrue(repository.lockSeat(7L, 1L, 50L, 300L));

        verify(values).setIfAbsent("lock:showtime:7:seat:1", "50", 300L, TimeUnit.SECONDS);
        verifyNoMoreInteractions(values);
    }

    @Test
    void ut02CheckoutRequests900SecondsThroughOwnerCheckedPromotion() {
        Seat seat = seat(1L, 100L, "STANDARD");
        when(seats.findByIds(List.of(1L))).thenReturn(List.of(seat));
        when(locks.promoteLockIfOwner(7L, 1L, 50L, 900L)).thenReturn(true);

        assertEquals(List.of(seat), service.promoteLocksForCheckout(7L, List.of(1L), 100L, 50L));

        var order = inOrder(tickets, locks);
        order.verify(tickets).validateSeatsNotSold(7L, List.of(1L));
        order.verify(locks).promoteLockIfOwner(7L, 1L, 50L, 900L);
        verifyNoMoreInteractions(locks);
    }

    @Test
    void ut02CheckoutRejectsLockNotOwnedByRequesterWithoutUnlockingIt() {
        when(seats.findByIds(List.of(1L))).thenReturn(List.of(seat(1L, 100L, "STANDARD")));
        // Repository contract returns false for another owner or a missing/expired lock.
        when(locks.promoteLockIfOwner(7L, 1L, 51L, 900L)).thenReturn(false);

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.promoteLocksForCheckout(7L, List.of(1L), 100L, 51L));

        assertEquals(ErrorCode.SEAT_ALREADY_BOOKED, error.getCode());
        verify(locks).promoteLockIfOwner(7L, 1L, 51L, 900L);
        verifyNoMoreInteractions(locks);
    }

    @Test
    void ut02CheckoutFailureReleasesOnlyPreviouslyPromotedSeats() {
        when(seats.findByIds(List.of(1L, 2L))).thenReturn(List.of(seat(1L, 100L, "STANDARD"),
                seat(2L, 100L, "STANDARD")));
        when(locks.promoteLockIfOwner(7L, 1L, 50L, 900L)).thenReturn(true);
        when(locks.promoteLockIfOwner(7L, 2L, 50L, 900L)).thenReturn(false);

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.promoteLocksForCheckout(7L, List.of(1L, 2L), 100L, 50L));

        assertEquals(ErrorCode.SEAT_ALREADY_BOOKED, error.getCode());
        var order = inOrder(locks);
        order.verify(locks).promoteLockIfOwner(7L, 1L, 50L, 900L);
        order.verify(locks).promoteLockIfOwner(7L, 2L, 50L, 900L);
        order.verify(locks).unlock(7L, 1L);
        verifyNoMoreInteractions(locks);
    }

    @Test
    void ut02DeselectOwnerUnlocksSeat() {
        when(locks.getLockedBy(7L, 1L)).thenReturn("50");

        service.deselectSeat(7L, 1L, 50L);

        verify(locks).getLockedBy(7L, 1L);
        verify(locks).unlock(7L, 1L);
        verifyNoMoreInteractions(locks);
    }

    @Test
    void ut02DeselectOtherOwnerIsForbiddenAndNeverUnlocks() {
        when(locks.getLockedBy(7L, 1L)).thenReturn("50");

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.deselectSeat(7L, 1L, 51L));

        assertEquals(ErrorCode.FORBIDDEN, error.getCode());
        verify(locks).getLockedBy(7L, 1L);
        verifyNoMoreInteractions(locks);
    }

    @Test
    void ut02DeselectMissingLockIsNoOp() {
        when(locks.getLockedBy(7L, 1L)).thenReturn(null);

        assertDoesNotThrow(() -> service.deselectSeat(7L, 1L, 50L));

        verify(locks).getLockedBy(7L, 1L);
        verifyNoMoreInteractions(locks);
    }

    @Test
    void ut03WrongRoomIsRejectedBeforeSoldCheckOrLock() {
        when(seats.findByIds(List.of(1L))).thenReturn(List.of(seat(1L, 200L, "STANDARD")));

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.preLockSeats(7L, List.of(1L), 100L, 50L));

        assertEquals(ErrorCode.VALIDATION_FAILED, error.getCode());
        verifyNoInteractions(tickets, locks);
    }

    @Test
    void ut03AisleIsRejectedBeforeSoldCheckOrLock() {
        when(seats.findByIds(List.of(1L))).thenReturn(List.of(seat(1L, 100L, "AISLE")));

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.preLockSeats(7L, List.of(1L), 100L, 50L));

        assertEquals(ErrorCode.VALIDATION_FAILED, error.getCode());
        verifyNoInteractions(tickets, locks);
    }

    @Test
    void ut03DuplicateIdsAreRejectedBeforeRepositoryAccess() {
        BusinessException error = assertThrows(BusinessException.class,
                () -> service.preLockSeats(7L, List.of(1L, 1L), 100L, 50L));

        assertEquals(ErrorCode.VALIDATION_FAILED, error.getCode());
        verifyNoInteractions(seats, tickets, locks);
    }

    @Test
    void ut03SoldSeatRejectionPreventsAnyLockAttempt() {
        when(seats.findByIds(List.of(1L))).thenReturn(List.of(seat(1L, 100L, "STANDARD")));
        BusinessException sold = new BusinessException("Sold seat", ErrorCode.SEAT_ALREADY_BOOKED);
        doThrow(sold).when(tickets).validateSeatsNotSold(7L, List.of(1L));

        assertSame(sold, assertThrows(BusinessException.class,
                () -> service.preLockSeats(7L, List.of(1L), 100L, 50L)));

        verify(tickets).validateSeatsNotSold(7L, List.of(1L));
        verifyNoInteractions(locks);
    }

    @Test
    void ut03OtherOwnerLockConflictReturnsFalseWithoutUnlockingConflictingSeat() {
        when(seats.findByIds(List.of(1L))).thenReturn(List.of(seat(1L, 100L, "STANDARD")));
        // SET NX conflict is represented by false; this unit does not execute Redis ownership logic.
        when(locks.lockSeat(7L, 1L, 51L, 300L)).thenReturn(false);

        assertFalse(service.preLockSeats(7L, List.of(1L), 100L, 51L));

        verify(tickets).validateSeatsNotSold(7L, List.of(1L));
        verify(locks).lockSeat(7L, 1L, 51L, 300L);
        verifyNoMoreInteractions(locks);
    }

    @Test
    void ut09RepeatedOwnerReleaseSkipsMissingAndOtherOwnerLocks() {
        when(locks.getLockedBy(7L, 1L)).thenReturn("50", null);
        when(locks.getLockedBy(7L, 2L)).thenReturn("51");
        when(locks.getLockedBy(7L, 3L)).thenReturn(null);

        service.releaseSeatLocksByOwner(7L, List.of(1L, 2L, 3L), 50L);
        service.releaseSeatLocksByOwner(7L, List.of(1L, 2L, 3L), 50L);

        verify(locks, times(2)).getLockedBy(7L, 1L);
        verify(locks, times(2)).getLockedBy(7L, 2L);
        verify(locks, times(2)).getLockedBy(7L, 3L);
        verify(locks, times(1)).unlock(7L, 1L);
        verifyNoMoreInteractions(locks);
    }

    private Seat seat(Long id, Long roomId, String typeName) {
        SeatType type = new SeatType();
        type.setTypeName(typeName);
        Seat seat = new Seat();
        seat.setSeatId(id);
        seat.setRoomId(roomId);
        seat.setSeatType(type);
        seat.setSeatRow(BigDecimal.ONE);
        seat.setSeatCol(BigDecimal.valueOf(id));
        return seat;
    }
}
