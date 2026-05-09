package com.uit.backend_cinema.modules.auth.infrastructure.persistence;

import java.util.Optional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;

import com.uit.backend_cinema.modules.auth.domain.entity.RefreshToken;
import com.uit.backend_cinema.modules.auth.domain.repository.RefreshTokenRepository;
import com.uit.backend_cinema.modules.auth.infrastructure.entity.RefreshTokenJpaEntity;
import com.uit.backend_cinema.modules.auth.infrastructure.entity.UserJpaEntity;
import com.uit.backend_cinema.modules.auth.infrastructure.mapper.RefreshTokenInfraMapper;
import com.uit.backend_cinema.modules.auth.infrastructure.repository.JpaRefreshTokenRepository;

@Repository
public class RefreshTokenRepositoryImpl implements RefreshTokenRepository {
    private final JpaRefreshTokenRepository jpaRefreshTokenRepository;
    private final RefreshTokenInfraMapper mapper;

    @PersistenceContext
    private EntityManager entityManager;

    public RefreshTokenRepositoryImpl(
            JpaRefreshTokenRepository jpaRefreshTokenRepository,
            RefreshTokenInfraMapper mapper
    ) {
        this.jpaRefreshTokenRepository = jpaRefreshTokenRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<RefreshToken> findByToken(String token) {
        return jpaRefreshTokenRepository.findByToken(token)
                .map(mapper::toDomain);
    }

    @Override
    public void save(RefreshToken refreshToken) {
        RefreshTokenJpaEntity jpaEntity = mapper.toInfrastructure(refreshToken);

        // Nếu có userId thì gắn user entity vào
        if (refreshToken.getUserId() != null) {
            jpaEntity.setUser(entityManager.getReference(UserJpaEntity.class, refreshToken.getUserId()));
        }

        jpaRefreshTokenRepository.save(jpaEntity);
    }

    @Override
    public void delete(RefreshToken refreshToken) {
        // Xóa trực tiếp theo token string — tránh lỗi detached entity
        jpaRefreshTokenRepository.deleteByToken(refreshToken.getToken());
    }
}
