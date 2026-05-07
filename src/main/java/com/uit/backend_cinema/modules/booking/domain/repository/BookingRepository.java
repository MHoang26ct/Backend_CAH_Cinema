package com.uit.backend_cinema.modules.booking.domain.repository;

import com.uit.backend_cinema.modules.booking.domain.entity.Booking;
import com.uit.backend_cinema.modules.booking.domain.entity.BookingStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingRepository {
    Booking save(Booking booking);

    Optional<Booking> findById(Long bookingId);

    Optional<Booking> findByIdForUpdate(Long bookingId);

    List<Booking> findByStatusAndExpiresAtBefore(BookingStatus status, LocalDateTime threshold);

    int markExpiredIfPendingAndExpired(Long bookingId, LocalDateTime now);
}
