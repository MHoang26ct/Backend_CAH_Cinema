package com.uit.backend_cinema.modules.auth.domain.repository;

import com.uit.backend_cinema.modules.auth.domain.entity.User;

import java.util.Optional;

public interface UserRepository {
    Optional<User> findById(Long userId);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    User save(User user);

    void softDelete(Long userId);
}
