package com.uit.backend_cinema.modules.ticket.infrastructure.mapper;

import com.uit.backend_cinema.modules.ticket.domain.entity.Ticket;
import com.uit.backend_cinema.modules.ticket.infrastructure.entity.TicketJpaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TicketInfraMapper {
    Ticket toDomain(TicketJpaEntity entity);

    TicketJpaEntity toEntity(Ticket ticket);
}
