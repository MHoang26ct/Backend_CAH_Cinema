package com.uit.backend_cinema.modules.auth.api.controller;

import com.uit.backend_cinema.common.util.ApiResponse;
import com.uit.backend_cinema.modules.auth.api.dto.*;
import com.uit.backend_cinema.modules.auth.application.AuthUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthUseCase authUseCase;

    public AuthController(AuthUseCase authUseCase) {
        this.authUseCase = authUseCase;
    }

    // ĐĂNG KÝ
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequestDTO request) {
        AuthResponseDTO response = authUseCase.register(
                request.getEmail(),
                request.getPassword(),
                request.getName(),
                request.getPhone()
        );
        return ResponseEntity.ok(ApiResponse.success(response, "Đăng ký thành công"));
    }

    // ĐĂNG NHẬP (EMAIL)
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDTO request) {
        AuthResponseDTO response = authUseCase.login(request.getEmail(), request.getPassword());
        return ResponseEntity.ok(ApiResponse.success(response, "Đăng nhập thành công"));
    }

    // ĐĂNG NHẬP (GOOGLE)
    @PostMapping("/google")
    public ResponseEntity<?> googleLogin(@Valid @RequestBody GoogleLoginRequestDTO request) {
        AuthResponseDTO response = authUseCase.loginWithGoogle(request.getIdToken());
        return ResponseEntity.ok(ApiResponse.success(response, "Đăng nhập Google thành công"));
    }

    // REFRESH TOKEN
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody RefreshTokenDTO request) {
        Map<String, String> response = authUseCase.refreshAccessToken(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ĐĂNG XUẤT
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody RefreshTokenDTO request) {
        authUseCase.logout(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success(null, "Đăng xuất thành công"));
    }
}
