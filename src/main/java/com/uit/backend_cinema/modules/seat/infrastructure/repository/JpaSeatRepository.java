package com.uit.backend_cinema.modules.seat.infrastructure.repository;

import com.uit.backend_cinema.modules.seat.infrastructure.entity.SeatJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
public interface JpaSeatRepository extends JpaRepository<SeatJpaEntity, Long> {
    List<SeatJpaEntity> findByRoomIdAndIsDeletedFalse(Long roomId);
}
