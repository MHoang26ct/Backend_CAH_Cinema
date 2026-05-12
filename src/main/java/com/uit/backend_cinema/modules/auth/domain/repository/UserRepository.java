package com.uit.backend_cinema.modules.auth.domain.repository;

import java.math.BigDecimal;
import java.util.Optional;

import com.uit.backend_cinema.modules.auth.domain.entity.User;

public interface UserRepository {
    Optional<User> findById(Long userId);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    User save(User user);

    void softDelete(Long userId);

    /**
     * Cập nhật một phần thông tin profile (name, email, phone).
     * Chỉ cập nhật field nào khác null.
     */
    User updateProfile(Long userId, String name, String email, String phone);

    /**
     * Cộng {@code amount} vào total_paid và tính lại total_point + rank_level.
     * Thực thi bằng 1 câu UPDATE duy nhất (không load entity).
     */
    void accumulatePaidAndRecalcRank(Long userId, BigDecimal amount);
}


