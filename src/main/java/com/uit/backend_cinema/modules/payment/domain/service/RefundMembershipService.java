package com.uit.backend_cinema.modules.payment.domain.service;

import com.uit.backend_cinema.modules.auth.domain.entity.AccountType;
import com.uit.backend_cinema.modules.auth.domain.repository.UserRepository;
import com.uit.backend_cinema.modules.booking.domain.entity.Booking;
import com.uit.backend_cinema.modules.booking.domain.repository.BookingRepository;
import com.uit.backend_cinema.modules.payment.domain.entity.Refund;
import com.uit.backend_cinema.modules.payment.domain.entity.RefundStatus;
import com.uit.backend_cinema.modules.payment.domain.repository.RefundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Reflects verified refund outcomes in the member's net spend, points and rank.
 *
 * A refund is locked and marked in the same transaction as the member update. This
 * prevents worker retries and duplicate provider events from changing points twice.
 * A later provider-side failure is compensated exactly once if a previous success
 * had already been reflected.
 */
@Service
@RequiredArgsConstructor
public class RefundMembershipService {
    private final RefundRepository refunds;
    private final BookingRepository bookings;
    private final UserRepository users;
    private final Clock clock;

    @Transactional
    public void reconcile(long refundId) {
        Refund refund = refunds.findByIdForUpdate(refundId).orElseThrow();
        if (refund.getStatus() == RefundStatus.SUCCEEDED) {
            applySuccessfulRefund(refund);
        } else if (refund.getStatus() == RefundStatus.FAILED) {
            reverseFailedRefund(refund);
        }
    }

    private void applySuccessfulRefund(Refund refund) {
        if (refund.getMembershipAdjustedAt() != null) return;

        Booking booking = bookings.findById(refund.getBookingId()).orElseThrow();
        if (!Boolean.TRUE.equals(booking.getIsPointsAccumulated()) || booking.getUserId() == null) return;

        users.findById(booking.getUserId()).ifPresent(user -> {
            if (user.getAccountType() != AccountType.WALK_IN && !Boolean.TRUE.equals(user.getIsDeleted())) {
                users.deductPaidAndRecalcRank(booking.getUserId(), refund.getAmount());
                refund.setMembershipAdjustedAt(LocalDateTime.now(clock));
                refunds.save(refund);
            }
        });
    }

    private void reverseFailedRefund(Refund refund) {
        if (refund.getMembershipAdjustedAt() == null || refund.getMembershipAdjustmentReversedAt() != null) return;

        Booking booking = bookings.findById(refund.getBookingId()).orElseThrow();
        if (booking.getUserId() == null) return;

        users.findById(booking.getUserId()).ifPresent(user -> {
            if (user.getAccountType() != AccountType.WALK_IN && !Boolean.TRUE.equals(user.getIsDeleted())) {
                users.accumulatePaidAndRecalcRank(booking.getUserId(), refund.getAmount());
                refund.setMembershipAdjustmentReversedAt(LocalDateTime.now(clock));
                refunds.save(refund);
            }
        });
    }
}
