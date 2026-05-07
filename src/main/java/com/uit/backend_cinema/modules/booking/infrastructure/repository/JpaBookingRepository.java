package com.uit.backend_cinema.modules.booking.infrastructure.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.uit.backend_cinema.modules.booking.domain.entity.BookingStatus;
import com.uit.backend_cinema.modules.booking.infrastructure.entity.BookingJpaEntity;

public interface JpaBookingRepository extends JpaRepository<BookingJpaEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from BookingJpaEntity b where b.bookingId = :bookingId")
    Optional<BookingJpaEntity> findByIdForUpdate(@Param("bookingId") Long bookingId);

    List<BookingJpaEntity> findByStatusAndExpiresAtBefore(BookingStatus status, LocalDateTime threshold);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            update bookings
            set status = 'EXPIRED', updated_at = :now, version = version + 1
            where booking_id = :bookingId
              and status = 'PENDING'
              and expires_at < :now
              and is_deleted = false
            """, nativeQuery = true)
    int markExpiredIfPendingAndExpired(@Param("bookingId") Long bookingId, @Param("now") LocalDateTime now);
}
