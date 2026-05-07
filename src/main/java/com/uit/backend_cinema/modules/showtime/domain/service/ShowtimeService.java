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
import java.util.Objects;

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

    @Transactional
    public void createShowtime(Showtime newShowtime) {
        validateShowtimePayload(newShowtime);
        if (newShowtime.getStatus() == null) {
            newShowtime.setStatus(ShowtimeStatus.AVAILABLE);
        }
        if (newShowtime.getIsDeleted() == null) {
            newShowtime.setIsDeleted(false);
        }
        if (isValidShowtime(newShowtime, true)) {
            showtimeRepository.save(newShowtime);
        }
    }

    @Transactional
    public void updateShowtime(Showtime newShowtime) {
        validateShowtimePayload(newShowtime);
        Showtime existingShowtime = getById(newShowtime.getShowtimeId());
        newShowtime.setIsDeleted(existingShowtime.getIsDeleted());
        if (isValidShowtime(newShowtime, false)) {
            showtimeRepository.save(newShowtime);
        }
    }

    @Transactional
    public void deleteShowtime(Long showtimeId) {
        Showtime existingShowtime = getById(showtimeId);
        existingShowtime.setIsDeleted(true);
        showtimeRepository.save(existingShowtime);
    }

    @Transactional
    public void changeStatusToSoldOut(Long showtimeId) {
        Showtime existingShowtime = getById(showtimeId);
        existingShowtime.setStatus(ShowtimeStatus.SOLD_OUT);
        showtimeRepository.save(existingShowtime);
    }

    private boolean isValidShowtime(Showtime newShowtime, boolean isCreate) {
        validateRequiredFields(newShowtime, isCreate);
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

        LocalTime startTime = newShowtime.getStartTime().toLocalTime();
        LocalTime endTime = newShowtime.getEndTime().toLocalTime();

        if (startTime.isBefore(LocalTime.of(8, 0)) || startTime.isAfter(LocalTime.MAX)) {
            throw new BusinessException("Suất chiếu phải bắt đầu từ 08:00 đến 23:59", ErrorCode.VALIDATION_FAILED);
        }
        if (!newShowtime.getEndTime().isAfter(newShowtime.getStartTime())) {
            throw new BusinessException("Suất chiếu phải kết thúc sau khi bắt đầu", ErrorCode.VALIDATION_FAILED);
        }
        if (endTime.isAfter(LocalTime.of(2, 0)) && endTime.isBefore(LocalTime.of(8, 0))) {
            throw new BusinessException("Suất chiếu phải kết thúc trước 02:00 ngày hôm sau", ErrorCode.VALIDATION_FAILED);
        }

        LocalDateTime startDateTime = newShowtime.getStartTime();
        LocalDateTime endDateTime = newShowtime.getEndTime();

        LocalDate startDate = startDateTime.toLocalDate();

        LocalDateTime boundaryNextDay = startDate.plusDays(1).atTime(2, 0);
        LocalDateTime morningNextDay = startDate.plusDays(1).atTime(8, 0);

        if (endDateTime.isAfter(boundaryNextDay) && endDateTime.isBefore(morningNextDay)) {
            throw new BusinessException("Suất chiếu phải kết thúc trước 02:00 ngày hôm sau", ErrorCode.VALIDATION_FAILED);
        }

        if (endDateTime.isAfter(morningNextDay) || endDateTime.isEqual(morningNextDay)) {
            throw new BusinessException("Suất chiếu không được kéo dài quá 08:00 ngày hôm sau", ErrorCode.VALIDATION_FAILED);
        }

        List<Showtime> existingShowtimes = showtimeRepository.findAllByRoomIdAndDate(
                newShowtime.getRoomId(),
                newShowtime.getStartTime().toLocalDate());
        for (Showtime showtime : existingShowtimes) {
            if (!isCreate && Objects.equals(showtime.getShowtimeId(), newShowtime.getShowtimeId())) {
                continue;
            }

            LocalDateTime existingStart = showtime.getStartTime();
            LocalDateTime existingEnd = showtime.getEndTime();
            if (existingStart.isBefore(endDateTime) && existingEnd.isAfter(startDateTime)) {
                throw new BusinessException("Suất chiếu trùng lặp với suất chiếu hiện có", ErrorCode.VALIDATION_FAILED);
            }
            if (endDateTime.plusMinutes(30).isAfter(existingStart) && startDateTime.minusMinutes(30).isBefore(existingEnd)) {
                throw new BusinessException("Khoảng cách giữa 2 suất chiếu phải ít nhất 30 phút", ErrorCode.VALIDATION_FAILED);
            }
        }
        if (startDateTime.toLocalDate().isAfter(LocalDate.now().plusDays(30))) {
            throw new BusinessException("Suất chiếu không được tạo trước 30 ngày", ErrorCode.VALIDATION_FAILED);
        }
        return true;
    }

    private void validateShowtimePayload(Showtime showtime) {
        if (showtime == null) {
            throw new BusinessException("Thông tin suất chiếu không được trống", ErrorCode.VALIDATION_FAILED);
        }
    }

    private void validateRequiredFields(Showtime showtime, boolean isCreate) {
        if (!isCreate && showtime.getShowtimeId() == null) {
            throw new BusinessException("ID suất chiếu không được trống", ErrorCode.VALIDATION_FAILED);
        }
        if (showtime.getMovieId() == null) {
            throw new BusinessException("ID phim không được trống", ErrorCode.VALIDATION_FAILED);
        }
        if (showtime.getRoomId() == null) {
            throw new BusinessException("ID phòng chiếu không được trống", ErrorCode.VALIDATION_FAILED);
        }
        if (showtime.getStartTime() == null) {
            throw new BusinessException("Thời gian bắt đầu không được trống", ErrorCode.VALIDATION_FAILED);
        }
        if (showtime.getEndTime() == null) {
            throw new BusinessException("Thời gian kết thúc không được trống", ErrorCode.VALIDATION_FAILED);
        }
        if (showtime.getFormat() == null) {
            throw new BusinessException("Định dạng suất chiếu không được trống", ErrorCode.VALIDATION_FAILED);
        }
        if (showtime.getBasePrice() == null) {
            throw new BusinessException("Giá vé cơ bản không được trống", ErrorCode.VALIDATION_FAILED);
        }
    }
}
