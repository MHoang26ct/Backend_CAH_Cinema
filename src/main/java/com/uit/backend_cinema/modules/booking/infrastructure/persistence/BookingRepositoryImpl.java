package com.uit.backend_cinema.modules.booking.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.uit.backend_cinema.modules.booking.domain.entity.Booking;
import com.uit.backend_cinema.modules.booking.domain.entity.BookingStatus;
import com.uit.backend_cinema.modules.booking.domain.repository.BookingRepository;
import com.uit.backend_cinema.modules.booking.infrastructure.mapper.BookingInfraMapper;
import com.uit.backend_cinema.modules.booking.infrastructure.repository.JpaBookingRepository;

@Repository
public class BookingRepositoryImpl implements BookingRepository {
    private final JpaBookingRepository jpaBookingRepository;
    private final BookingInfraMapper mapper;

    public BookingRepositoryImpl(JpaBookingRepository jpaBookingRepository, BookingInfraMapper mapper) {
        this.jpaBookingRepository = jpaBookingRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<Booking> findByIdForUpdate(Long bookingId) {
        return jpaBookingRepository.findByIdForUpdate(bookingId)
                .map(mapper::toDomain);
    }

    @Override
    public Booking save(Booking booking) {
        return mapper.toDomain(jpaBookingRepository.save(mapper.toEntity(booking)));
    }

    @Override
    public Optional<Booking> findById(Long bookingId) {
        return jpaBookingRepository.findById(bookingId).map(mapper::toDomain);
    }

    @Override
    public List<Booking> findByStatusAndExpiresAtBefore(BookingStatus status, LocalDateTime threshold) {
        return jpaBookingRepository.findByStatusAndExpiresAtBefore(status, threshold)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public int markExpiredIfPendingAndExpired(Long bookingId, LocalDateTime now) {
        return jpaBookingRepository.markExpiredIfPendingAndExpired(bookingId, now);
    }
}
