package com.uit.backend_cinema.modules.booking.infrastructure.persistence;

import com.uit.backend_cinema.modules.booking.domain.entity.Booking;
import com.uit.backend_cinema.modules.booking.domain.entity.BookingStatus;
import com.uit.backend_cinema.modules.booking.domain.repository.BookingRepository;
import com.uit.backend_cinema.modules.booking.infrastructure.mapper.BookingInfraMapper;
import com.uit.backend_cinema.modules.booking.infrastructure.repository.JpaBookingRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class BookingRepositoryImpl implements BookingRepository {
    private final JpaBookingRepository jpaBookingRepository;
    private final BookingInfraMapper mapper;

    public BookingRepositoryImpl(JpaBookingRepository jpaBookingRepository, BookingInfraMapper mapper) {
        this.jpaBookingRepository = jpaBookingRepository;
        this.mapper = mapper;
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
}
