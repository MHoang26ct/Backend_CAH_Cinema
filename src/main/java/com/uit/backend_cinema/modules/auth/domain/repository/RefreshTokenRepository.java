package com.uit.backend_cinema.modules.auth.domain.repository;

import java.util.Optional;

import com.uit.backend_cinema.modules.auth.domain.entity.RefreshToken;

public interface RefreshTokenRepository {
    Optional<RefreshToken> findByToken(String token);
    void save(RefreshToken refreshToken);
    void delete(RefreshToken refreshToken);
}
