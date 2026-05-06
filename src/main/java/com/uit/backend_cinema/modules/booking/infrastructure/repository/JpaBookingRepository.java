package com.uit.backend_cinema.modules.booking.infrastructure.repository;

import com.uit.backend_cinema.modules.booking.domain.entity.BookingStatus;
import com.uit.backend_cinema.modules.booking.infrastructure.entity.BookingJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface JpaBookingRepository extends JpaRepository<BookingJpaEntity, Long> {
    List<BookingJpaEntity> findByStatusAndExpiresAtBefore(BookingStatus status, LocalDateTime threshold);
}
