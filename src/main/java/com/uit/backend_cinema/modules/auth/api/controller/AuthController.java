package com.uit.backend_cinema.modules.auth.api.controller;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.uit.backend_cinema.common.sercurity.CustomUserDetailsService;
import com.uit.backend_cinema.common.util.ApiResponse;
import com.uit.backend_cinema.common.util.JwtUtil;
import com.uit.backend_cinema.modules.auth.api.dto.*;
import com.uit.backend_cinema.modules.auth.api.mapper.UserApiMapper;
import com.uit.backend_cinema.modules.auth.domain.entity.RefreshToken;
import com.uit.backend_cinema.modules.auth.domain.entity.User;
import com.uit.backend_cinema.modules.auth.domain.service.AuthService;
import com.uit.backend_cinema.modules.auth.domain.service.RefreshTokenService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;
    private final UserApiMapper userApiMapper;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    public AuthController(
            AuthService authService,
            RefreshTokenService refreshTokenService,
            JwtUtil jwtUtil,
            CustomUserDetailsService userDetailsService,
            UserApiMapper userApiMapper
    ) {
        this.authService = authService;
        this.refreshTokenService = refreshTokenService;
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.userApiMapper = userApiMapper;
    }

    // ĐĂNG KÝ
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequestDTO request) {
        User user = authService.register(
                request.getEmail(),
                request.getPassword(),
                request.getName(),
                request.getPhone()
        );

        // Tạo token cho user vừa đăng ký
        AuthResponseDTO response = generateAuthResponse(user);
        return ResponseEntity.ok(ApiResponse.success(response, "Đăng ký thành công"));
    }

    // ĐĂNG NHẬP (EMAIL)
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDTO request) {
        User user = authService.login(request.getEmail(), request.getPassword());

        AuthResponseDTO response = generateAuthResponse(user);
        return ResponseEntity.ok(ApiResponse.success(response, "Đăng nhập thành công"));
    }

    // ĐĂNG NHẬP (GOOGLE)
    @PostMapping("/google")
    public ResponseEntity<?> googleLogin(@Valid @RequestBody GoogleLoginRequestDTO request) {
        try {
            // Xác minh ID Token từ Google
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(), GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(request.getIdToken());
            if (idToken == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(400, "Google ID Token không hợp lệ"));
            }

            // Lấy thông tin user từ token
            GoogleIdToken.Payload payload = idToken.getPayload();
            String email = payload.getEmail();
            String name = (String) payload.get("name");
            String avatarUrl = (String) payload.get("picture");
            String providerId = payload.getSubject();

            // Đăng nhập hoặc tạo tài khoản mới
            User user = authService.loginOrRegisterWithGoogle(email, name, avatarUrl, providerId);

            AuthResponseDTO response = generateAuthResponse(user);
            return ResponseEntity.ok(ApiResponse.success(response, "Đăng nhập Google thành công"));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, "Lỗi xác thực Google: " + e.getMessage()));
        }
    }

    // REFRESH TOKEN
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody RefreshTokenDTO request) {
        String requestRefreshToken = request.getRefreshToken();

        return refreshTokenService.findByToken(requestRefreshToken)
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUserId)
                .map(userId -> {
                    UserDetails userDetails = userDetailsService.loadUserById(userId);

                    Map<String, Object> extraClaims = new HashMap<>();
                    extraClaims.put("userId", userId);
                    String newAccessToken = jwtUtil.generateToken(extraClaims, userDetails);

                    Map<String, String> responseData = new HashMap<>();
                    responseData.put("accessToken", newAccessToken);
                    responseData.put("refreshToken", requestRefreshToken);
                    return ResponseEntity.ok(ApiResponse.success(responseData));
                })
                .orElseThrow(() -> new RuntimeException("Refresh token không hợp lệ"));
    }

    // ĐĂNG XUẤT
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody RefreshTokenDTO request) {
        String requestRefreshToken = request.getRefreshToken();
         return refreshTokenService.findByToken(requestRefreshToken)
                .map(token -> {
                    refreshTokenService.deleteByRefreshToken(token);
                    return ResponseEntity.ok(ApiResponse.success(null, "Đăng xuất thành công"));
                })
                .orElseThrow(() -> new RuntimeException("Refresh token không hợp lệ"));
    }

    // HÀM TIỆN ÍCH
    private AuthResponseDTO generateAuthResponse(User user) {
        // Lấy thông tin UserDetails để tạo JWT
        UserDetails userDetails = userDetailsService.loadUserById(user.getUserId());

        // Tạo access token với userId trong claims
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("userId", user.getUserId());
        String accessToken = jwtUtil.generateToken(extraClaims, userDetails);

        // Tạo refresh token
        String refreshToken = refreshTokenService.createRefreshToken(user.getUserId());

        // Build response
        return AuthResponseDTO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(userApiMapper.toDto(user))
                .build();
    }
}
