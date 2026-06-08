package com.uit.backend_cinema.modules.ticket.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.uit.backend_cinema.modules.booking.domain.entity.BookingStatus;
import com.uit.backend_cinema.modules.ticket.domain.entity.Ticket;
import com.uit.backend_cinema.modules.ticket.domain.repository.TicketRepository;
import com.uit.backend_cinema.modules.ticket.infrastructure.mapper.TicketInfraMapper;
import com.uit.backend_cinema.modules.ticket.infrastructure.repository.JpaTicketRepository;

@Repository
public class TicketRepositoryImpl implements TicketRepository {
    private final JpaTicketRepository jpaTicketRepository;
    private final TicketInfraMapper mapper;

    public TicketRepositoryImpl(JpaTicketRepository jpaTicketRepository, TicketInfraMapper mapper) {
        this.jpaTicketRepository = jpaTicketRepository;
        this.mapper = mapper;
    }

    @Override
    public boolean existsByBookingIdAndSeatId(Long bookingId, Long seatId) {
        return jpaTicketRepository.existsByBookingIdAndSeatId(bookingId, seatId);
    }

    @Override
    public int saveAll(List<Ticket> tickets) {
        return jpaTicketRepository.saveAll(tickets.stream().map(mapper::toEntity).toList()).stream()
                .map(mapper::toDomain).toList().size();
    }

    @Override
    public List<Ticket> findAllByBookingId(Long bookingId) {
        return jpaTicketRepository.findByBookingId(bookingId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsSoldSeatByShowtimeIdAndSeatIds(Long showtimeId, List<Long> seatIds) {
        return seatIds != null
                && !seatIds.isEmpty()
                && jpaTicketRepository.existsByShowtimeIdAndSeatIdIn(showtimeId, seatIds);
    }

    @Override
    public List<Long> findSoldSeatIdsByShowtimeId(Long showtimeId) {
        return jpaTicketRepository.findSoldSeatIdsByShowtimeId(
                showtimeId,
                List.of(BookingStatus.PAID, BookingStatus.CHECKED_IN)
        );
    }

    @Override
    public Optional<Ticket> findById(Long ticketId) {
        return jpaTicketRepository.findById(ticketId).map(mapper::toDomain);
    }

    @Override
    public Ticket save(Ticket ticket) {
        var entity = mapper.toEntity(ticket);
        var savedEntity = jpaTicketRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }
}
