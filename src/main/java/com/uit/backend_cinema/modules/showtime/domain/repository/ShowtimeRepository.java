package com.uit.backend_cinema.modules.showtime.domain.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.uit.backend_cinema.modules.showtime.domain.entity.CinemaShowtimes;
import com.uit.backend_cinema.modules.showtime.domain.entity.MovieShowtimes;
import com.uit.backend_cinema.modules.showtime.domain.entity.Showtime;

public interface ShowtimeRepository {
    Optional<Showtime> findById(Long showtimeId);
    MovieShowtimes findShowtimesByMovieId(Long movieId, LocalDate date);
    void save(Showtime showtime);
    void softDeleteByRoomId(Long roomId);
    void softDeleteByRoomIds(List<Long> roomIds);
    void softDeleteByMovieId(Long movieId);
    List<Showtime> findAllByRoomIdAndDate(Long roomId, LocalDate date);
    List<CinemaShowtimes> findShowtimesByCinemaId(Long cinemaId, LocalDate date);

    /** Tìm showtime AVAILABLE của phòng trong khoảng thời gian (dùng cho batch cancel) */
    List<Showtime> findActiveByRoomIdBetweenDates(Long roomId, LocalDateTime from, LocalDateTime to);

    /** Chuyển toàn bộ showtime chưa diễn ra (> cutoff) sang room mới (dùng cho room cloning) */
    int updateRoomIdForShowtimesAfterDate(Long oldRoomId, Long newRoomId, LocalDateTime after);

    /** Tìm thời điểm kết thúc muộn nhất của showtime trong phòng (dùng cho cleanup scheduler) */
    Optional<LocalDateTime> findMaxEndTimeByRoomId(Long roomId);
}
