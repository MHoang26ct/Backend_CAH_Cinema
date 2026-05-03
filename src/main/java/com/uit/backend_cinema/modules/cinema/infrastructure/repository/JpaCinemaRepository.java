package com.uit.backend_cinema.modules.cinema.infrastructure.repository;

import com.uit.backend_cinema.modules.cinema.infrastructure.entity.CinemaJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaCinemaRepository extends JpaRepository<CinemaJpaEntity, Long> {
}
