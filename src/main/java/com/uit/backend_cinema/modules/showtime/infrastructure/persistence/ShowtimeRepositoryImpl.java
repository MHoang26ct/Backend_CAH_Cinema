package com.uit.backend_cinema.modules.showtime.infrastructure.persistence;

import com.uit.backend_cinema.modules.showtime.domain.entity.CinemaShowtimes;
import com.uit.backend_cinema.modules.showtime.domain.entity.MovieShowtimes;
import com.uit.backend_cinema.modules.showtime.domain.entity.Showtime;
import com.uit.backend_cinema.modules.showtime.domain.repository.ShowtimeRepository;
import com.uit.backend_cinema.modules.showtime.infrastructure.mapper.ShowtimeInfraMapper;
import com.uit.backend_cinema.modules.showtime.infrastructure.repository.JpaShowtimeRepository;
import com.uit.backend_cinema.modules.showtime.infrastructure.repository.ShowtimeReadRepository;
import com.uit.backend_cinema.modules.showtime.infrastructure.repository.dto.CinemaShowtimeRowDto;
import com.uit.backend_cinema.modules.showtime.infrastructure.repository.dto.MovieShowtimeRowDto;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class ShowtimeRepositoryImpl implements ShowtimeRepository {

    private final JpaShowtimeRepository jpaShowtimeRepository;
    private final ShowtimeReadRepository showtimeReadRepository;
    private final ShowtimeInfraMapper mapper;

    public ShowtimeRepositoryImpl(
            JpaShowtimeRepository jpaShowtimeRepository,
            ShowtimeReadRepository showtimeReadRepository,
            ShowtimeInfraMapper mapper
    ) {
        this.jpaShowtimeRepository = jpaShowtimeRepository;
        this.showtimeReadRepository = showtimeReadRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<Showtime> findById(Long showtimeId) {
        return jpaShowtimeRepository.findById(showtimeId)
                .map(mapper::toDomain);
    }

    @Override
    public void save(Showtime showtime) {
        jpaShowtimeRepository.save(mapper.toJpaEntity(showtime));
    }

    @Override
    public void softDeleteByRoomId(Long roomId) {
        jpaShowtimeRepository.softDeleteByRoomId(roomId);
    }

    @Override
    public void softDeleteByRoomIds(List<Long> roomIds) {
        if (roomIds == null || roomIds.isEmpty()) {
            return;
        }
        jpaShowtimeRepository.softDeleteByRoomIds(roomIds);
    }

    @Override
    public void softDeleteByMovieId(Long movieId) {
        jpaShowtimeRepository.softDeleteByMovieId(movieId);
    }

    @Override
    public List<Showtime> findAllByRoomIdAndDate(Long roomId, LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();
        return jpaShowtimeRepository.findAllByRoomIdAndDate(roomId, startOfDay, endOfDay).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public MovieShowtimes findShowtimesByMovieId(Long movieId, LocalDate date) {
        // Query toàn bộ suất chiếu của 1 phim trong ngày [00:00, 00:00 ngày kế tiếp)
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();
        List<MovieShowtimeRowDto> projections = showtimeReadRepository.findMovieShowtimeRowsByDate(movieId, startOfDay, endOfDay);

        if (projections.isEmpty()) {
            return null;
        }

        // Cùng movieId nên có thể lấy bản ghi đầu làm thông tin phim đại diện
        MovieShowtimes.MovieInfo movieInfo = mapper.toMovieInfo(projections.get(0));

        // Nhóm theo rạp để output trả về theo cấu trúc: movie -> list cinemas -> list showtimes
        Map<Long, List<MovieShowtimeRowDto>> groupedByCinema = projections.stream()
                .collect(Collectors.groupingBy(MovieShowtimeRowDto::getCinemaId));

        List<MovieShowtimes.CinemaShowtimes> cinemas = groupedByCinema.values().stream()
                .map(showtimesByCinema -> {
                    MovieShowtimeRowDto rep = showtimesByCinema.get(0);
                    // Mỗi phần tử trong nhóm là 1 suất chiếu của cùng rạp
                    List<MovieShowtimes.ShowtimeInfo> showtimes = showtimesByCinema.stream()
                            .map(mapper::toMovieShowtimeInfo)
                            .toList();

                    MovieShowtimes.CinemaShowtimes cinemaShowtimes = new MovieShowtimes.CinemaShowtimes();
                    cinemaShowtimes.setCinemaId(rep.getCinemaId());
                    cinemaShowtimes.setCinemaName(rep.getCinemaName());
                    cinemaShowtimes.setAddress(rep.getAddress());
                    cinemaShowtimes.setShowtimes(showtimes);
                    return cinemaShowtimes;
                })
                .toList();

        MovieShowtimes result = new MovieShowtimes();
        result.setMovie(movieInfo);
        result.setCinemas(cinemas);
        return result;
    }

    @Override
    public List<CinemaShowtimes> findShowtimesByCinemaId(Long cinemaId, LocalDate date) {
        // Query toàn bộ suất chiếu trong 1 rạp theo ngày
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();
        List<CinemaShowtimeRowDto> projections = showtimeReadRepository.findCinemaShowtimeRowsByDate(cinemaId, startOfDay, endOfDay);

        // Nhóm theo phim để output trả về theo cấu trúc: list movies + showtimes của từng movie
        Map<Long, List<CinemaShowtimeRowDto>> groupedByMovie = projections.stream()
                .collect(Collectors.groupingBy(CinemaShowtimeRowDto::getMovieId));

        return groupedByMovie.values().stream()
                .map(showtimesByMovie -> {
                    CinemaShowtimeRowDto rep = showtimesByMovie.get(0);
                    // Mỗi phần tử trong nhóm là 1 suất chiếu của cùng phim
                    List<CinemaShowtimes.ShowtimeInfo> showtimes = showtimesByMovie.stream()
                            .map(mapper::toCinemaShowtimeInfo)
                            .toList();

                    CinemaShowtimes result = new CinemaShowtimes();
                    result.setMovie(mapper.toCinemaMovieInfo(rep));
                    result.setShowtimes(showtimes);
                    return result;
                })
                .toList();
    }
}
