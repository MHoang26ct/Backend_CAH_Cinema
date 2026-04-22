package com.uit.backend_cinema.modules.auth.infrastructure.persistence;

import com.uit.backend_cinema.modules.auth.domain.entity.RefreshToken;
import com.uit.backend_cinema.modules.auth.domain.repository.RefreshTokenRepository;
import com.uit.backend_cinema.modules.auth.infrastructure.entity.RefreshTokenJpaEntity;
import com.uit.backend_cinema.modules.auth.infrastructure.entity.UserJpaEntity;
import com.uit.backend_cinema.modules.auth.infrastructure.mapper.RefreshTokenInfraMapper;
import com.uit.backend_cinema.modules.auth.infrastructure.repository.JpaRefreshTokenRepository;
import com.uit.backend_cinema.modules.auth.infrastructure.repository.JpaUserRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class RefreshTokenRepositoryImpl implements RefreshTokenRepository {
    private final JpaRefreshTokenRepository jpaRefreshTokenRepository;
    private final JpaUserRepository jpaUserRepository;
    private final RefreshTokenInfraMapper mapper;

    public RefreshTokenRepositoryImpl(
            JpaRefreshTokenRepository jpaRefreshTokenRepository,
            JpaUserRepository jpaUserRepository,
            RefreshTokenInfraMapper mapper
    ) {
        this.jpaRefreshTokenRepository = jpaRefreshTokenRepository;
        this.jpaUserRepository = jpaUserRepository;
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
            UserJpaEntity user = jpaUserRepository.findById(refreshToken.getUserId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy user với id: " + refreshToken.getUserId()));
            jpaEntity.setUser(user);
        }

        RefreshTokenJpaEntity saved = jpaRefreshTokenRepository.save(jpaEntity);
    }

    @Override
    public void delete(RefreshToken refreshToken) {
        RefreshTokenJpaEntity jpaEntity = mapper.toInfrastructure(refreshToken);
        jpaRefreshTokenRepository.delete(jpaEntity);
    }
}
