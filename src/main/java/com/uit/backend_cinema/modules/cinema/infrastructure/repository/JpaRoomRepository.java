package com.uit.backend_cinema.modules.cinema.infrastructure.repository;

import com.uit.backend_cinema.modules.cinema.infrastructure.entity.RoomJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JpaRoomRepository extends JpaRepository<RoomJpaEntity, Long> {
    List<RoomJpaEntity> findAllByCinemaId(long cinemaId);
}
