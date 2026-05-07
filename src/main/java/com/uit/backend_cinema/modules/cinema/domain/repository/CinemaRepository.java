package com.uit.backend_cinema.modules.cinema.domain.repository;

import java.util.List;
import java.util.Optional;

import com.uit.backend_cinema.modules.cinema.domain.entity.Cinema;

public interface CinemaRepository {
    Optional<Cinema> findById(long cinemaId);
    List<Cinema> findAll();
    Cinema save(Cinema cinema);
    void delete(Cinema cinema);
}
