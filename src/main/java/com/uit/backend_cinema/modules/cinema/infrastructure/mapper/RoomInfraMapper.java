package com.uit.backend_cinema.modules.cinema.infrastructure.mapper;

import com.uit.backend_cinema.modules.cinema.domain.entity.Room;
import com.uit.backend_cinema.modules.cinema.infrastructure.entity.RoomJpaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RoomInfraMapper {
    Room toDomain(RoomJpaEntity entity);
    RoomJpaEntity toEntity(Room domain);
}
