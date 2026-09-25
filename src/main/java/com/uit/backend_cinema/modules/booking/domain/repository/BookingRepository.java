package com.uit.backend_cinema.modules.booking.domain.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.uit.backend_cinema.modules.booking.domain.entity.Booking;
import com.uit.backend_cinema.modules.booking.domain.entity.BookingStatus;

public interface BookingRepository {
    Booking save(Booking booking);

    boolean hasScheduleBlockingBookings(Long showtimeId, LocalDateTime now);

    Optional<Booking> findById(Long bookingId);

    Optional<Booking> findByIdForUpdate(Long bookingId);

    List<Booking> findByStatusAndExpiresAtLessThanEqual(BookingStatus status, LocalDateTime threshold);

    int markExpiredIfPendingAndExpired(Long bookingId, LocalDateTime now);

    /** Tìm tất cả booking PAID hoặc PENDING của 1 showtime (dùng khi hủy showtime) */
    List<Booking> findActiveByShowtimeId(Long showtimeId);
}
