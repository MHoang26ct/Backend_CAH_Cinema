package com.uit.backend_cinema.modules.auth.api.controller;

import com.uit.backend_cinema.common.util.ApiResponse;
import com.uit.backend_cinema.modules.auth.api.dto.*;
import com.uit.backend_cinema.modules.auth.api.dto.request.*;
import com.uit.backend_cinema.modules.auth.api.dto.response.AuthResponseDTO;
import com.uit.backend_cinema.modules.auth.api.dto.response.FP_VerifyOtpResponseOTP;
import com.uit.backend_cinema.modules.auth.application.AuthUseCase;
import com.uit.backend_cinema.modules.notification.domain.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthUseCase authUseCase;
    private final NotificationService notificationService;

    public AuthController(AuthUseCase authUseCase, NotificationService notificationService) {
        this.notificationService = notificationService;
        this.authUseCase = authUseCase;
    }

    // GỬI OTP
    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@Valid @RequestBody SendOtpRequestDTO requestDTO) {
        notificationService.sendOtp(requestDTO.getEmail());
        return ResponseEntity.ok(ApiResponse.success(null, "Gửi OTP thành công"));
    }

    // XÁC THỰC OTP
    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@Valid @RequestBody VerifyOtpRequestDTO requestDTO) {
        boolean isVerified = notificationService.verifyOtp(requestDTO.getEmail(), requestDTO.getOtp());
        return ResponseEntity.ok(ApiResponse.success(isVerified, "Xác thực OTP thành công"));
    }

    @PostMapping("/fp-verify-otp")
    public ResponseEntity<?> verifyOtpForgetPassword(@Valid @RequestBody VerifyOtpRequestDTO requestDTO) {
        boolean isVerified = notificationService.verifyOtp(requestDTO.getEmail(), requestDTO.getOtp());
        if (isVerified) {
            FP_VerifyOtpResponseOTP response = authUseCase.generateResetToken(requestDTO.getEmail());
            return ResponseEntity.ok(ApiResponse.success(response, "Xác thực OTP thành công"));
        }
        return ResponseEntity.ok(ApiResponse.success(null, "Xác thực OTP thất bại"));
    }

    @PostMapping("/fp-change-password")
    public ResponseEntity<?> changePasswordForget(@Valid @RequestBody ChangePasswordForgetRequestDTO requestDTO) {
        authUseCase.changePassword_Forget(requestDTO.getEmail(), requestDTO.getNewPassword(), requestDTO.getResetToken());
        return ResponseEntity.ok(ApiResponse.success(null, "Đổi mật khẩu thành công"));
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequestDTO requestDTO) {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        authUseCase.changePassword(email, requestDTO.getOldPassword(), requestDTO.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success(null, "Đổi mật khẩu thành công"));
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
