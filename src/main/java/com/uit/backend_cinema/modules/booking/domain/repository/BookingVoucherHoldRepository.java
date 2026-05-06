package com.uit.backend_cinema.modules.booking.domain.repository;

import com.uit.backend_cinema.modules.booking.domain.entity.BookingVoucherHold;
import com.uit.backend_cinema.modules.booking.domain.entity.BookingVoucherHoldStatus;

import java.time.LocalDateTime;
import java.util.Optional;

public interface BookingVoucherHoldRepository {
    BookingVoucherHold save(BookingVoucherHold hold);

    // Dùng để tính quota voucher còn lại: quantity - used_count - active_holds
    long countByVoucherIdAndStatusAndExpiresAtAfter(Long voucherId, BookingVoucherHoldStatus status, LocalDateTime now);

    Optional<BookingVoucherHold> findByBookingId(Long bookingId);

    void softDeleteByBookingId(Long bookingId);
}
