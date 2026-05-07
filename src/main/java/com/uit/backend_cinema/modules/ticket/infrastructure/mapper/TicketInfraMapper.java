package com.uit.backend_cinema.modules.ticket.infrastructure.mapper;

import org.mapstruct.Mapper;

import com.uit.backend_cinema.modules.ticket.domain.entity.Ticket;
import com.uit.backend_cinema.modules.ticket.infrastructure.entity.TicketJpaEntity;

@Mapper(componentModel = "spring")
public interface TicketInfraMapper {
    Ticket toDomain(TicketJpaEntity entity);

    TicketJpaEntity toEntity(Ticket ticket);
}
