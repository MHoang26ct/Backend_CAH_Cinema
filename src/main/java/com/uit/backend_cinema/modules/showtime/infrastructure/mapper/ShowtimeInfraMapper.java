package com.uit.backend_cinema.modules.showtime.infrastructure.mapper;

import org.mapstruct.Mapper;

import com.uit.backend_cinema.modules.showtime.domain.entity.CinemaShowtimes;
import com.uit.backend_cinema.modules.showtime.domain.entity.MovieShowtimes;
import com.uit.backend_cinema.modules.showtime.domain.entity.Showtime;
import com.uit.backend_cinema.modules.showtime.infrastructure.entity.ShowtimeJpaEntity;
import com.uit.backend_cinema.modules.showtime.infrastructure.repository.dto.CinemaShowtimeRowDto;
import com.uit.backend_cinema.modules.showtime.infrastructure.repository.dto.MovieShowtimeRowDto;

@Mapper(componentModel = "spring")
public interface  ShowtimeInfraMapper {
    Showtime toDomain(ShowtimeJpaEntity entity);
    ShowtimeJpaEntity toJpaEntity(Showtime showtime);

    MovieShowtimes.MovieInfo toMovieInfo(MovieShowtimeRowDto projection);
    MovieShowtimes.ShowtimeInfo toMovieShowtimeInfo(MovieShowtimeRowDto projection);

    CinemaShowtimes.MovieInfo toCinemaMovieInfo(CinemaShowtimeRowDto projection);
    CinemaShowtimes.ShowtimeInfo toCinemaShowtimeInfo(CinemaShowtimeRowDto projection);
}
