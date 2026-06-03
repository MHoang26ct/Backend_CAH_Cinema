package com.uit.backend_cinema.modules.cinema.infrastructure.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.uit.backend_cinema.modules.cinema.infrastructure.entity.PendingRoomCleanupJpaEntity;

public interface JpaPendingRoomCleanupRepository extends JpaRepository<PendingRoomCleanupJpaEntity, Long> {

    @Query("SELECT p FROM PendingRoomCleanupJpaEntity p WHERE p.cleanedUp = false")
    List<PendingRoomCleanupJpaEntity> findAllNotCleanedUp();

    boolean existsByOldRoomIdAndCleanedUpFalse(Long oldRoomId);
}
