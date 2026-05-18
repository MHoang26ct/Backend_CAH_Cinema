package com.uit.backend_cinema.modules.auth.api.controller;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.uit.backend_cinema.common.exception.BusinessException;
import com.uit.backend_cinema.common.exception.ErrorCode;
import com.uit.backend_cinema.common.util.ApiResponse;
import com.uit.backend_cinema.modules.auth.api.dto.request.UpdateProfileRequestDTO;
import com.uit.backend_cinema.modules.auth.application.UserUseCase;
import com.uit.backend_cinema.modules.auth.domain.repository.UserRepository;
import com.uit.backend_cinema.modules.auth.domain.entity.User;

/**
 * Controller xử lý các thao tác liên quan đến profile của người dùng.
 * Base path: /api/v1/users
 */
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserUseCase userUseCase;
    private final UserRepository userRepository;

    public UserController(UserUseCase userUseCase, UserRepository userRepository) {
        this.userUseCase = userUseCase;
        this.userRepository = userRepository;
    }

    /**
     * GET /api/v1/users/me
     * Trả về thông tin profile + 5 booking gần nhất (đầy đủ thông tin hóa đơn).
     */
    @GetMapping("/me")
    public ResponseEntity<?> getProfile() {
        Long userId = resolveUserId();
        UserUseCase.UserProfileResponseBundle bundle = userUseCase.getProfile(userId);
        return ResponseEntity.ok(ApiResponse.success(bundle, "Lấy thông tin profile thành công"));
    }

    /**
     * PATCH /api/v1/users/me
     * Cập nhật name, email, phone (chỉ field được gửi lên mới thay đổi).
     */
    @PatchMapping("/me")
    public ResponseEntity<?> updateProfile(@Valid @RequestBody UpdateProfileRequestDTO requestDTO) {
        Long userId = resolveUserId();
        var updated = userUseCase.updateProfile(
                userId,
                requestDTO.getName(),
                requestDTO.getEmail(),
                requestDTO.getPhone(),
                requestDTO.getAvatarUrl());
        return ResponseEntity.ok(ApiResponse.success(updated, "Cập nhật profile thành công"));
    }

    // Lấy userId từ JWT (email trong SecurityContext → tra DB).
    private Long resolveUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new BusinessException("Chưa đăng nhập", ErrorCode.UNAUTHORIZED);
        }
        String email = auth.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("User không tồn tại", ErrorCode.RESOURCE_NOT_FOUND));
        return user.getUserId();
    }
}
