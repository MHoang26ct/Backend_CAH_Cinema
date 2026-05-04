package com.uit.backend_cinema.modules.showtime.domain.service;

import com.uit.backend_cinema.common.exception.BusinessException;
import com.uit.backend_cinema.common.exception.ErrorCode;
import com.uit.backend_cinema.modules.showtime.domain.entity.Showtime;
import com.uit.backend_cinema.modules.showtime.domain.repository.ShowtimeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@Transactional(readOnly = true)
public class ShowtimeService {
     private final ShowtimeRepository showtimeRepository;

    public ShowtimeService(ShowtimeRepository showtimeRepository) {
        this.showtimeRepository = showtimeRepository;
    }

    public Showtime getById(Long showtimeId) {
        return showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new BusinessException(
                        "Không tìm thấy suất chiếu", ErrorCode.RESOURCE_NOT_FOUND));
    }

    public Page<Showtime> findByMovieAndDate(Long movieId, LocalDate date, Pageable pageable) {
        return showtimeRepository.findByMovieAndDate(movieId, date, pageable);
    }

    public Page<Showtime> findByCinemaAndDate(Long cinemaId, LocalDate date, Pageable pageable) {
        return showtimeRepository.findByCinemaAndDate(cinemaId, date, pageable);
    }
}
