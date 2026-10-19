package com.uit.backend_cinema.unit_test;

import com.uit.backend_cinema.modules.auth.domain.entity.AccountType;
import com.uit.backend_cinema.modules.auth.domain.entity.User;
import com.uit.backend_cinema.modules.auth.domain.repository.UserRepository;
import com.uit.backend_cinema.modules.booking.domain.entity.Booking;
import com.uit.backend_cinema.modules.booking.domain.repository.BookingRepository;
import com.uit.backend_cinema.modules.payment.domain.entity.Refund;
import com.uit.backend_cinema.modules.payment.domain.entity.RefundStatus;
import com.uit.backend_cinema.modules.payment.domain.repository.RefundRepository;
import com.uit.backend_cinema.modules.payment.domain.service.RefundMembershipService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RefundMembershipServiceTest {
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-18T03:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
    private RefundRepository refunds;
    private BookingRepository bookings;
    private UserRepository users;
    private RefundMembershipService service;
    private Refund refund;
    private Booking booking;

    @BeforeEach void setup() {
        refunds = mock(RefundRepository.class);
        bookings = mock(BookingRepository.class);
        users = mock(UserRepository.class);
        service = new RefundMembershipService(refunds, bookings, users, clock);
        refund = Refund.builder().refundId(1L).bookingId(2L).amount(new BigDecimal("65856"))
                .status(RefundStatus.SUCCEEDED).build();
        booking = new Booking();
        booking.setBookingId(2L); booking.setUserId(3L); booking.setIsPointsAccumulated(true);
        User user = new User(); user.setUserId(3L); user.setAccountType(AccountType.REGISTERED); user.setIsDeleted(false);
        when(refunds.findByIdForUpdate(1L)).thenReturn(Optional.of(refund));
        when(bookings.findById(2L)).thenReturn(Optional.of(booking));
        when(users.findById(3L)).thenReturn(Optional.of(user));
    }

    @Test void successfulRefundDeductsExactRefundAmountOnce() {
        service.reconcile(1L);
        assertNotNull(refund.getMembershipAdjustedAt());
        verify(users).deductPaidAndRecalcRank(3L, new BigDecimal("65856"));
        verify(refunds).save(refund);

        service.reconcile(1L);
        verify(users, times(1)).deductPaidAndRecalcRank(3L, new BigDecimal("65856"));
    }

    @Test void successfulRefundBeforePointsWereAccruedDoesNotCreateFutureDebt() {
        booking.setIsPointsAccumulated(false);
        service.reconcile(1L);
        assertNull(refund.getMembershipAdjustedAt());
        verify(users, never()).deductPaidAndRecalcRank(anyLong(), any());
        verify(refunds, never()).save(any());
    }

    @Test void providerFailureAfterSuccessRestoresPointsExactlyOnce() {
        refund.setMembershipAdjustedAt(java.time.LocalDateTime.now(clock).minusMinutes(1));
        refund.setStatus(RefundStatus.FAILED);
        service.reconcile(1L);
        assertNotNull(refund.getMembershipAdjustmentReversedAt());
        verify(users).accumulatePaidAndRecalcRank(3L, new BigDecimal("65856"));

        service.reconcile(1L);
        verify(users, times(1)).accumulatePaidAndRecalcRank(3L, new BigDecimal("65856"));
    }

    @Test void walkInAccountsNeverReceiveMembershipAdjustment() {
        User walkIn = new User(); walkIn.setUserId(3L); walkIn.setAccountType(AccountType.WALK_IN);
        when(users.findById(3L)).thenReturn(Optional.of(walkIn));
        service.reconcile(1L);
        assertNull(refund.getMembershipAdjustedAt());
        verify(users, never()).deductPaidAndRecalcRank(anyLong(), any());
    }
}
