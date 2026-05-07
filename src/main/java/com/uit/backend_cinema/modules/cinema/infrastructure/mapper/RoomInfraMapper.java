package com.uit.backend_cinema.modules.cinema.infrastructure.mapper;

import org.mapstruct.Mapper;

import com.uit.backend_cinema.modules.cinema.domain.entity.Room;
import com.uit.backend_cinema.modules.cinema.infrastructure.entity.RoomJpaEntity;

@Mapper(componentModel = "spring")
public interface RoomInfraMapper {
    Room toDomain(RoomJpaEntity entity);
    RoomJpaEntity toEntity(Room domain);
}
