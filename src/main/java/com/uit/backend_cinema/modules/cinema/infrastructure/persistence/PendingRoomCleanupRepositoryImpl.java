package com.uit.backend_cinema.modules.cinema.infrastructure.persistence;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.uit.backend_cinema.modules.cinema.domain.entity.PendingRoomCleanup;
import com.uit.backend_cinema.modules.cinema.domain.repository.PendingRoomCleanupRepository;
import com.uit.backend_cinema.modules.cinema.infrastructure.entity.PendingRoomCleanupJpaEntity;
import com.uit.backend_cinema.modules.cinema.infrastructure.repository.JpaPendingRoomCleanupRepository;

@Repository
public class PendingRoomCleanupRepositoryImpl implements PendingRoomCleanupRepository {
    private final JpaPendingRoomCleanupRepository jpaRepository;

    public PendingRoomCleanupRepositoryImpl(JpaPendingRoomCleanupRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public PendingRoomCleanup save(PendingRoomCleanup cleanup) {
        PendingRoomCleanupJpaEntity entity = toEntity(cleanup);
        PendingRoomCleanupJpaEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public List<PendingRoomCleanup> findAllNotCleanedUp() {
        return jpaRepository.findAllNotCleanedUp().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public boolean existsByOldRoomIdAndCleanedUpFalse(Long oldRoomId) {
        return jpaRepository.existsByOldRoomIdAndCleanedUpFalse(oldRoomId);
    }

    private PendingRoomCleanupJpaEntity toEntity(PendingRoomCleanup domain) {
        PendingRoomCleanupJpaEntity entity = new PendingRoomCleanupJpaEntity();
        entity.setCleanupId(domain.getCleanupId());
        entity.setOldRoomId(domain.getOldRoomId());
        entity.setNewRoomId(domain.getNewRoomId());
        entity.setReplacedAt(domain.getReplacedAt());
        entity.setCleanedUp(domain.getCleanedUp());
        entity.setCleanedUpAt(domain.getCleanedUpAt());
        return entity;
    }

    private PendingRoomCleanup toDomain(PendingRoomCleanupJpaEntity entity) {
        PendingRoomCleanup domain = new PendingRoomCleanup();
        domain.setCleanupId(entity.getCleanupId());
        domain.setOldRoomId(entity.getOldRoomId());
        domain.setNewRoomId(entity.getNewRoomId());
        domain.setReplacedAt(entity.getReplacedAt());
        domain.setCleanedUp(entity.getCleanedUp());
        domain.setCleanedUpAt(entity.getCleanedUpAt());
        return domain;
    }
}
