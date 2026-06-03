package com.uit.backend_cinema.modules.showtime.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.uit.backend_cinema.modules.showtime.api.dto.CinemaShowtimesResponseDTO;
import com.uit.backend_cinema.modules.showtime.api.dto.CreateShowtimeDTO;
import com.uit.backend_cinema.modules.showtime.api.dto.MovieShowtimesResponseDTO;
import com.uit.backend_cinema.modules.showtime.api.dto.ShowtimeDetailDTO;
import com.uit.backend_cinema.modules.showtime.api.dto.UpdateShowtimeDTO;
import com.uit.backend_cinema.modules.showtime.domain.entity.CinemaShowtimes;
import com.uit.backend_cinema.modules.showtime.domain.entity.MovieShowtimes;
import com.uit.backend_cinema.modules.showtime.domain.entity.Showtime;

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

    /** Dùng cho admin: map Showtime domain -> ShowtimeDetailDTO (kèm status) */
    @Mapping(target = "movieTitle", ignore = true)
    @Mapping(target = "moviePosterUrl", ignore = true)
    @Mapping(target = "movieDuration", ignore = true)
    @Mapping(target = "roomName", ignore = true)
    @Mapping(target = "cinemaId", ignore = true)
    @Mapping(target = "cinemaName", ignore = true)
    @Mapping(source = "status", target = "status", defaultExpression = "java(source.getStatus() != null ? source.getStatus().name() : null)")
    ShowtimeDetailDTO toShowtimeDetailDTO(Showtime source);
}
