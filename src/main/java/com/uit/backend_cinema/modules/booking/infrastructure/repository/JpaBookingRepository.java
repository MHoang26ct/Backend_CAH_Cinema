package com.uit.backend_cinema.modules.booking.infrastructure.repository;

import com.uit.backend_cinema.modules.booking.domain.entity.BookingStatus;
import com.uit.backend_cinema.modules.booking.infrastructure.entity.BookingJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface JpaBookingRepository extends JpaRepository<BookingJpaEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from BookingJpaEntity b where b.bookingId = :bookingId")
    Optional<BookingJpaEntity> findByIdForUpdate(@Param("bookingId") Long bookingId);
    List<BookingJpaEntity> findByStatusAndExpiresAtBefore(BookingStatus status, LocalDateTime threshold);
}
