package com.uit.backend_cinema.modules.showtime.domain.service;

import com.uit.backend_cinema.common.exception.BusinessException;
import com.uit.backend_cinema.common.exception.ErrorCode;
import com.uit.backend_cinema.modules.movies.domain.service.MovieService;
import com.uit.backend_cinema.modules.showtime.domain.entity.CinemaShowtimes;
import com.uit.backend_cinema.modules.showtime.domain.entity.MovieShowtimes;
import com.uit.backend_cinema.modules.showtime.domain.entity.Showtime;
import com.uit.backend_cinema.modules.showtime.domain.entity.ShowtimeStatus;
import com.uit.backend_cinema.modules.showtime.domain.repository.ShowtimeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ShowtimeService {
    private final ShowtimeRepository showtimeRepository;
    private final MovieService movieService;

    public ShowtimeService(ShowtimeRepository showtimeRepository, MovieService movieService) {
        this.showtimeRepository = showtimeRepository;
        this.movieService = movieService;
    }

    public Showtime getById(Long showtimeId) {
        return showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new BusinessException(
                        "Không tìm thấy suất chiếu", ErrorCode.RESOURCE_NOT_FOUND));
    }

    public MovieShowtimes getShowtimesByMovieId(Long movieId, LocalDate date) {
        return showtimeRepository.findShowtimesByMovieId(movieId, date);
    }

    public List<CinemaShowtimes> getShowtimesByCinemaId(Long cinemaId, LocalDate date) {
        return showtimeRepository.findShowtimesByCinemaId(cinemaId, date);
    }

    public void createShowtime(Showtime newShowtime) {
        if (isValidShowtime(newShowtime, true)) {
            showtimeRepository.save(newShowtime);
        }
    }

    public void updateShowtime(Showtime newShowtime) {
        if (isValidShowtime(newShowtime, false)) {
            showtimeRepository.save(newShowtime);
        }
    }

    public void deleteShowtime(Long showtimeId) {
        Showtime existingShowtime = getById(showtimeId);
        existingShowtime.setIsDeleted(true);
        showtimeRepository.save(existingShowtime);
    }

    public void changeStatusToSoldOut(Long showtimeId) {
        Showtime existingShowtime = getById(showtimeId);
        existingShowtime.setStatus(ShowtimeStatus.SOLD_OUT);
        showtimeRepository.save(existingShowtime);
    }

    private boolean isValidShowtime(Showtime newShowtime, boolean isCreate) {
        if (isCreate) {
            try {
                movieService.getById(newShowtime.getMovieId());
            }
            catch (BusinessException ex) {
                if (ex.getCode() == ErrorCode.RESOURCE_NOT_FOUND) {
                    throw new BusinessException("Không tìm thấy phim với ID: " + newShowtime.getMovieId(), ErrorCode.RESOURCE_NOT_FOUND);
                }
                else {
                    throw ex;
                }
            }
        }

        LocalTime startTime = newShowtime.getStartTime().toLocalTime();
        LocalTime endTime = newShowtime.getEndTime().toLocalTime();

        if (startTime.isBefore(LocalTime.of(8, 0)) || startTime.isAfter(LocalTime.MAX)) {
            throw new BusinessException("Suất chiếu phải bắt đầu từ 08:00 đến 23:59", ErrorCode.VALIDATION_FAILED);
        }
        if (endTime.isBefore(startTime)) {
            throw new BusinessException("Suất chiếu phải kết thúc sau khi bắt đầu", ErrorCode.VALIDATION_FAILED);
        }
        if (endTime.isAfter(LocalTime.of(2, 0)) && endTime.isBefore(LocalTime.of(8, 0))) {
            throw new BusinessException("Suất chiếu phải kết thúc trước 02:00 ngày hôm sau", ErrorCode.VALIDATION_FAILED);
        }

        LocalDateTime startDateTime = newShowtime.getStartTime();
        LocalDateTime endDateTime = newShowtime.getEndTime();

        List<Showtime> existingShowtimes = showtimeRepository.findAllByMovieIdAndRoomIdAndDate(
                newShowtime.getMovieId(),
                newShowtime.getRoomId(),
                newShowtime.getStartTime().toLocalDate());
        for (Showtime showtime : existingShowtimes) {
            if (showtime.getStartTime().isBefore(endDateTime) && showtime.getEndTime().isAfter(startDateTime)) {
                throw new BusinessException("Suất chiếu trùng lặp với suất chiếu hiện có", ErrorCode.VALIDATION_FAILED);
            }
            else if (showtime.getStartTime().isAfter(startDateTime) && showtime.getStartTime().isBefore(endDateTime)) {
                throw new BusinessException("Suất chiếu trùng lặp với suất chiếu hiện có", ErrorCode.VALIDATION_FAILED);
            }
            if (endTime.plusMinutes(30).isAfter(showtime.getStartTime().toLocalTime())) {
                throw new BusinessException("Khoảng cách giữa 2 suất chiếu phải ít nhất 30 phút", ErrorCode.VALIDATION_FAILED);
            }
            else if (startTime.minusMinutes(30).isBefore(showtime.getEndTime().toLocalTime())) {
                throw new BusinessException("Khoảng cách giữa 2 suất chiếu phải ít nhất 30 phút", ErrorCode.VALIDATION_FAILED);
            }
        }
        if (startDateTime.toLocalDate().isAfter(LocalDate.now().plusDays(30))) {
            throw new BusinessException("Suất chiếu không được tạo trước 30 ngày", ErrorCode.VALIDATION_FAILED);
        }
        return true;
    }
}
