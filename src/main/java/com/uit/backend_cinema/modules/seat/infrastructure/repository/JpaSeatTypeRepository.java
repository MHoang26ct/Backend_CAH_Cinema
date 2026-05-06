package com.uit.backend_cinema.modules.seat.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

import com.uit.backend_cinema.modules.seat.infrastructure.entity.SeatTypeJpaEntity;

@Repository
public interface JpaSeatTypeRepository extends JpaRepository<SeatTypeJpaEntity, Long> {
    List<SeatTypeJpaEntity> findAll();
}
