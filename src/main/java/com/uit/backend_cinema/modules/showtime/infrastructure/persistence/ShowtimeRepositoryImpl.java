package com.uit.backend_cinema.modules.showtime.infrastructure.persistence;

import com.uit.backend_cinema.modules.showtime.domain.entity.Showtime;
import com.uit.backend_cinema.modules.showtime.domain.repository.ShowtimeRepository;
import com.uit.backend_cinema.modules.showtime.infrastructure.mapper.ShowtimeInfraMapper;
import com.uit.backend_cinema.modules.showtime.infrastructure.repository.JpaShowtimeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public class ShowtimeRepositoryImpl implements ShowtimeRepository {

    private final JpaShowtimeRepository jpaShowtimeRepository;
    private final ShowtimeInfraMapper mapper;
    private final ShowtimeEnricher enricher;

    public ShowtimeRepositoryImpl(
            JpaShowtimeRepository jpaShowtimeRepository,
            ShowtimeInfraMapper mapper,
            ShowtimeEnricher enricher
    ) {
        this.jpaShowtimeRepository = jpaShowtimeRepository;
        this.mapper = mapper;
        this.enricher = enricher;
    }

    @Override
    public Optional<Showtime> findById(Long showtimeId) {
        return jpaShowtimeRepository.findById(showtimeId)
                .map(mapper::toDomain)
                .map(enricher::enrich);
    }

    @Override
    public Page<Showtime> findByMovieAndDate(Long movieId, LocalDate date, Pageable pageable) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();
        return jpaShowtimeRepository.findByMovieAndDate(movieId, start, end, pageable)
                .map(mapper::toDomain)
                .map(enricher::enrich);
    }

    @Override
    public Page<Showtime> findByCinemaAndDate(Long cinemaId, LocalDate date, Pageable pageable) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();
        // Native query không hỗ trợ sort theo tên field Java → dùng unsorted, DB trả về theo start_time ASC
        Pageable unsorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.unsorted());
        return jpaShowtimeRepository.findByCinemaAndDate(cinemaId, start, end, unsorted)
                .map(mapper::toDomain)
                .map(enricher::enrich);
    }
}
