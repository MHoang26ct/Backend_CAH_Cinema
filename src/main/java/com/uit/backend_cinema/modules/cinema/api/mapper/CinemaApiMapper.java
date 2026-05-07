package com.uit.backend_cinema.modules.cinema.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.uit.backend_cinema.modules.cinema.api.entity.CinemaResponseDTO;
import com.uit.backend_cinema.modules.cinema.api.entity.CreateCinemaRequestDTO;
import com.uit.backend_cinema.modules.cinema.api.entity.CreateRoomRequestDTO;
import com.uit.backend_cinema.modules.cinema.api.entity.RoomResponseDTO;
import com.uit.backend_cinema.modules.cinema.api.entity.UpdateCinemaRequestDTO;
import com.uit.backend_cinema.modules.cinema.api.entity.UpdateRoomRequestDTO;
import com.uit.backend_cinema.modules.cinema.domain.entity.Cinema;
import com.uit.backend_cinema.modules.cinema.domain.entity.Room;

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
