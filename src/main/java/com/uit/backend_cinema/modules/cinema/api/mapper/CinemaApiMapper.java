package com.uit.backend_cinema.modules.cinema.api.mapper;

import com.uit.backend_cinema.modules.cinema.api.entity.*;
import com.uit.backend_cinema.modules.cinema.domain.entity.Cinema;
import com.uit.backend_cinema.modules.cinema.domain.entity.Room;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CinemaApiMapper {

    // Cinema mappings
    @Mapping(target = "cinemaId", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    Cinema toDomain(CreateCinemaRequestDTO request);

    @Mapping(target = "deleted", ignore = true)
    Cinema toDomain(UpdateCinemaRequestDTO request);

    CinemaResponseDTO toDTO(Cinema cinema);

    // Room mappings
    @Mapping(target = "roomId", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    Room toDomain(CreateRoomRequestDTO request);

    @Mapping(target = "cinemaId", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    Room toDomain(UpdateRoomRequestDTO request);

    RoomResponseDTO toDTO(Room room);
}
