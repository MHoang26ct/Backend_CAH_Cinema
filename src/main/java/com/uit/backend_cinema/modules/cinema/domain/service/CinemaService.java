package com.uit.backend_cinema.modules.cinema.domain.service;

import com.uit.backend_cinema.common.exception.BusinessException;
import com.uit.backend_cinema.common.exception.ErrorCode;
import com.uit.backend_cinema.modules.cinema.domain.entity.Cinema;
import com.uit.backend_cinema.modules.cinema.domain.repository.CinemaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CinemaService {

    private final CinemaRepository cinemaRepository;

    public CinemaService(CinemaRepository cinemaRepository) {
        this.cinemaRepository = cinemaRepository;
    }

    public Cinema findById(long cinemaId) {
        return cinemaRepository.findById(cinemaId)
                .orElseThrow(() -> new BusinessException("Rạp chiếu phim không tồn tại", ErrorCode.RESOURCE_NOT_FOUND));
    }

    public List<Cinema> findAll() {
        return cinemaRepository.findAll();
    }

    @Transactional
    public Cinema create(Cinema cinema) {
        return cinemaRepository.save(cinema);
    }

    @Transactional
    public Cinema update(Cinema updatedCinema) {
        Cinema existing = cinemaRepository.findById(updatedCinema.getCinemaId())
                .orElseThrow(() -> new BusinessException("Rạp chiếu phim không tồn tại", ErrorCode.RESOURCE_NOT_FOUND));
        existing.setName(updatedCinema.getName());
        existing.setAddress(updatedCinema.getAddress());
        existing.setHotline(updatedCinema.getHotline());
        return cinemaRepository.save(existing);
    }

    @Transactional
    public void delete(long cinemaId) {
        Cinema existing = cinemaRepository.findById(cinemaId)
                .orElseThrow(() -> new BusinessException("Rạp chiếu phim không tồn tại", ErrorCode.RESOURCE_NOT_FOUND));
        existing.setDeleted(true);
        cinemaRepository.save(existing);
    }
}
