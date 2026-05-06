package com.uit.backend_cinema.modules.showtime.api.mapper;

import com.uit.backend_cinema.modules.showtime.api.dto.*;
import com.uit.backend_cinema.modules.showtime.domain.entity.CinemaShowtimes;
import com.uit.backend_cinema.modules.showtime.domain.entity.MovieShowtimes;
import com.uit.backend_cinema.modules.showtime.domain.entity.Showtime;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ShowtimeApiMapper {

    @Mapping(target = "showtimeId", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "status", ignore = true)
    Showtime toDomain(CreateShowtimeDTO createShowtimeDTO);

    @Mapping(target = "isDeleted", ignore = true)
    Showtime toDomain(UpdateShowtimeDTO updateShowtimeDTO);

    MovieShowtimesResponseDTO toMovieShowtimesResponseDto(MovieShowtimes source);

    CinemaShowtimesResponseDTO toCinemaShowtimesResponseDto(CinemaShowtimes source);
}
