package com.uit.backend_cinema.modules.cinema.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.uit.backend_cinema.modules.cinema.infrastructure.entity.CinemaJpaEntity;

@Repository
public interface JpaCinemaRepository extends JpaRepository<CinemaJpaEntity, Long> {
}
