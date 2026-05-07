package com.uit.backend_cinema.modules.auth.application;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.uit.backend_cinema.common.exception.BusinessException;
import com.uit.backend_cinema.common.exception.ErrorCode;
import com.uit.backend_cinema.common.util.JwtUtil;
import com.uit.backend_cinema.modules.auth.api.dto.response.AuthResponseDTO;
import com.uit.backend_cinema.modules.auth.api.dto.UserDTO;
import com.uit.backend_cinema.modules.auth.api.dto.response.FP_VerifyOtpResponseOTP;
import com.uit.backend_cinema.modules.auth.api.mapper.UserApiMapper;
import com.uit.backend_cinema.modules.auth.domain.entity.RefreshToken;
import com.uit.backend_cinema.modules.auth.domain.entity.User;
import com.uit.backend_cinema.modules.auth.domain.service.AuthService;
import com.uit.backend_cinema.modules.auth.domain.service.RefreshTokenService;
import com.uit.backend_cinema.modules.auth.infrastructure.security.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Application Use Case — điều phối các domain service, JWT util, mapper.
 * Đây là nơi chứa toàn bộ "orchestration logic" của auth module.
 * Controller chỉ gọi các method ở đây và wrap ApiResponse.
 */
@Service
public class AuthUseCase {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    private final CustomUserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;
    private final UserApiMapper userApiMapper;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    public AuthUseCase(
            AuthService authService,
            RefreshTokenService refreshTokenService,
            CustomUserDetailsService userDetailsService,
            JwtUtil jwtUtil,
            UserApiMapper userApiMapper
    ) {
        this.authService = authService;
        this.refreshTokenService = refreshTokenService;
        this.userDetailsService = userDetailsService;
        this.jwtUtil = jwtUtil;
        this.userApiMapper = userApiMapper;
    }

    // ĐĂNG KÝ
    public AuthResponseDTO register(String email, String rawPassword, String name, String phone) {
        User user = authService.register(email, rawPassword, name, phone);
        return buildAuthResponse(user);
    }

    // ĐĂNG NHẬP (EMAIL)
    public AuthResponseDTO login(String email, String rawPassword) {
        User user = authService.login(email, rawPassword);
        return buildAuthResponse(user);
    }

    // ĐĂNG NHẬP (GOOGLE)
    public AuthResponseDTO loginWithGoogle(String idToken) {
        GoogleIdToken googleIdToken = verifyGoogleToken(idToken);

        GoogleIdToken.Payload payload = googleIdToken.getPayload();
        String email      = payload.getEmail();
        String name       = (String) payload.get("name");
        String avatarUrl  = (String) payload.get("picture");
        String providerId = payload.getSubject();

        User user = authService.loginOrRegisterWithGoogle(email, name, avatarUrl, providerId);
        return buildAuthResponse(user);
    }

    // Tạo token reset password
    public FP_VerifyOtpResponseOTP generateResetToken(String email) {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("Purpose", "Change password");

        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

        String resetToken = jwtUtil.generateToken(extraClaims, userDetails);
        FP_VerifyOtpResponseOTP response = new FP_VerifyOtpResponseOTP();
        response.setToken(resetToken);
        return response;
    }

    // Đổi mật khẩu (quên mật khẩu)
    public void changePassword_Forget(String email, String newPassword, String token) {
        try {
            if (!jwtUtil.verifyResetToken(token, email)) {
                throw new BusinessException("Token không hợp lệ", ErrorCode.TOKEN_INVALID);
            }
            authService.resetPassword(email, newPassword);
        }
        catch (BusinessException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new BusinessException("Token không hợp lệ", ErrorCode.TOKEN_INVALID);
        }
    }

    // Đổi mật khẩu
    public void changePassword(String email, String oldPassword, String newPassword) {
        authService.changePasswordWithOldPassword(email, oldPassword, newPassword);
    }

    // REFRESH TOKEN
    public Map<String, String> refreshAccessToken(String refreshTokenStr) {
        Long userId = refreshTokenService.findByToken(refreshTokenStr)
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUserId)
                .orElseThrow(() -> new BusinessException(
                        "Refresh token không hợp lệ", ErrorCode.TOKEN_INVALID));

        UserDetails userDetails = userDetailsService.loadUserById(userId);

        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("userId", userId);
        String newAccessToken = jwtUtil.generateToken(extraClaims, userDetails);

        Map<String, String> result = new HashMap<>();
        result.put("accessToken", newAccessToken);
        result.put("refreshToken", refreshTokenStr);
        return result;
    }

    // ĐĂNG XUẤT
    public void logout(String refreshTokenStr) {
        refreshTokenService.findByToken(refreshTokenStr)
                .ifPresentOrElse(
                        refreshTokenService::deleteByRefreshToken,
                        () -> { throw new BusinessException(
                                "Refresh token không hợp lệ", ErrorCode.TOKEN_INVALID); }
                );
    }

    // HÀM NỘI BỘ

    /**
     * Tạo AuthResponseDTO hoàn chỉnh (accessToken + refreshToken + user info).
     * Chỉ gọi sau khi user đã được xác thực / tạo mới thành công.
     */
    private AuthResponseDTO buildAuthResponse(User user) {
        UserDetails userDetails = userDetailsService.loadUserById(user.getUserId());

        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("userId", user.getUserId());
        String accessToken = jwtUtil.generateToken(extraClaims, userDetails);

        String refreshToken = refreshTokenService.createRefreshToken(user.getUserId());

        UserDTO userDTO = userApiMapper.toDto(user);

        return AuthResponseDTO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(userDTO)
                .build();
    }

    // Xác minh Google ID Token — ném BusinessException nếu không hợp lệ
    private GoogleIdToken verifyGoogleToken(String idTokenStr) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(), GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenStr);
            if (idToken == null) {
                throw new BusinessException("Google ID Token không hợp lệ", ErrorCode.INVALID_GOOGLE_TOKEN);
            }
            return idToken;

        } catch (BusinessException e) {
            throw e; // Re-throw để GlobalExceptionHandler xử lý
        } catch (Exception e) {
            throw new BusinessException(
                    "Không thể xác minh Google ID Token: " + e.getMessage(),
                    ErrorCode.INVALID_GOOGLE_TOKEN
            );
        }
    }
}
