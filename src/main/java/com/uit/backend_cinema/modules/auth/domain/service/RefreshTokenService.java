package com.uit.backend_cinema.modules.auth.domain.service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uit.backend_cinema.common.exception.BusinessException;
import com.uit.backend_cinema.common.exception.ErrorCode;
import com.uit.backend_cinema.modules.auth.domain.entity.RefreshToken;
import com.uit.backend_cinema.modules.auth.domain.repository.RefreshTokenRepository;

@Service
@Transactional(readOnly = true)
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
    @Transactional(noRollbackFor = BusinessException.class)
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.isExpired()) {
            refreshTokenRepository.delete(token);
            throw new BusinessException("Token đã hết hạn. Vui lòng đăng nhập lại.", ErrorCode.TOKEN_EXPIRED);
        }
        return token;
    }

    // Xóa token khi đăng xuất
    @Transactional
    public void deleteByRefreshToken(RefreshToken token) {
        refreshTokenRepository.delete(token);
    }
}
