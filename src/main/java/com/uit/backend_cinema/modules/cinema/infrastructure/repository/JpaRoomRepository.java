package com.uit.backend_cinema.modules.cinema.infrastructure.repository;

import com.uit.backend_cinema.modules.cinema.infrastructure.entity.RoomJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JpaRoomRepository extends JpaRepository<RoomJpaEntity, Long> {
    List<RoomJpaEntity> findAllByCinemaId(long cinemaId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update RoomJpaEntity r set r.deleted = true where r.cinemaId = :cinemaId and r.deleted = false")
    void softDeleteByCinemaId(@Param("cinemaId") long cinemaId);
}
