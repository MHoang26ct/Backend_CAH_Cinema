package com.uit.backend_cinema.modules.auth.domain.service;

import com.uit.backend_cinema.common.exception.TokenRefreshException;
import com.uit.backend_cinema.modules.auth.domain.entity.RefreshToken;
import com.uit.backend_cinema.modules.auth.domain.repository.RefreshTokenRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {
    // Refresh token expiration time in seconds (30 days)
    private final long REFRESH_TOKEN_EXPIRATION = 30L * 24 * 3600;

    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    // Tạo token mới khi người dùng đăng nhập
    @Transactional
    public String createRefreshToken(Long userId) {
        RefreshToken refreshToken = new RefreshToken();

        refreshToken.setUserId(userId);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiryDate(LocalDateTime.now().plusSeconds(REFRESH_TOKEN_EXPIRATION));

        refreshTokenRepository.save(refreshToken);

        return refreshToken.getToken();
    }

    // Tìm token trong db
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    // Kiểm tra token đã hết hạn chưa
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.isExpired()) {
            refreshTokenRepository.delete(token); // Nếu đã hết hạn thì xóa luôn
            throw new TokenRefreshException(token.getToken(), "Token đã hết hạn. Vui lòng đăng nhập lại.");
        }
        return token;
    }

    // Xóa token khi đăng xuất
    public void deleteByRefreshToken(RefreshToken token) {
        refreshTokenRepository.delete(token);
    }
}
