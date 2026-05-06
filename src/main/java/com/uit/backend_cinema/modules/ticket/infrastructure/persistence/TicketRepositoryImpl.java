package com.uit.backend_cinema.modules.ticket.infrastructure.persistence;

import com.uit.backend_cinema.modules.ticket.domain.entity.Ticket;
import com.uit.backend_cinema.modules.ticket.domain.repository.TicketRepository;
import com.uit.backend_cinema.modules.ticket.infrastructure.mapper.TicketInfraMapper;
import com.uit.backend_cinema.modules.ticket.infrastructure.repository.JpaTicketRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

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
}
