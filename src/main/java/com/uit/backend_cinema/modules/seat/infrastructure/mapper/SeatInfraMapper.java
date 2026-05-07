package com.uit.backend_cinema.modules.seat.infrastructure.mapper;

import com.uit.backend_cinema.modules.seat.domain.entity.Seat;
import com.uit.backend_cinema.modules.seat.domain.entity.SeatType;
import com.uit.backend_cinema.modules.seat.infrastructure.entity.SeatJpaEntity;
import com.uit.backend_cinema.modules.seat.infrastructure.entity.SeatTypeJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SeatInfraMapper {
    @Mapping(target = "isLocked", ignore = true) // DB không có field này, set sau từ Redis
    @Mapping(target = "isSold", ignore = true)
    @Mapping(target = "occupancyStatus", ignore = true)
    Seat toDomain(SeatJpaEntity entity);

    SeatType toDomain(SeatTypeJpaEntity entity);

    SeatJpaEntity toEntity(Seat seat);
}
