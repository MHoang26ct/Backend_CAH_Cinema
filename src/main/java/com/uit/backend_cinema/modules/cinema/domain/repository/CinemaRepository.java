package com.uit.backend_cinema.modules.cinema.domain.repository;

import com.uit.backend_cinema.modules.cinema.domain.entity.Cinema;

import java.util.List;
import java.util.Optional;

public interface CinemaRepository {
    Optional<Cinema> findById(long cinemaId);
    List<Cinema> findAll();
    Cinema save(Cinema cinema);
    void delete(Cinema cinema);
}
