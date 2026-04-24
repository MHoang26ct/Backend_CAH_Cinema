package com.uit.backend_cinema.modules.auth.infrastructure.repository;

import com.uit.backend_cinema.modules.auth.infrastructure.entity.RefreshTokenJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JpaRefreshTokenRepository extends JpaRepository<RefreshTokenJpaEntity, Long> {
    Optional<RefreshTokenJpaEntity> findByToken(String token);
    void deleteByToken(String token);
}
