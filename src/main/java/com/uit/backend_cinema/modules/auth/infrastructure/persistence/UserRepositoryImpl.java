package com.uit.backend_cinema.modules.auth.infrastructure.persistence;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Repository;

import com.uit.backend_cinema.modules.auth.domain.entity.User;
import com.uit.backend_cinema.modules.auth.domain.repository.UserRepository;
import com.uit.backend_cinema.modules.auth.infrastructure.entity.RoleJpaEntity;
import com.uit.backend_cinema.modules.auth.infrastructure.entity.UserJpaEntity;
import com.uit.backend_cinema.modules.auth.infrastructure.mapper.UserInfraMapper;
import com.uit.backend_cinema.modules.auth.infrastructure.repository.JpaRoleRepository;
import com.uit.backend_cinema.modules.auth.infrastructure.repository.JpaUserRepository;

@Repository
public class UserRepositoryImpl implements UserRepository {
    private final JpaUserRepository jpaUserRepository;
    private final JpaRoleRepository jpaRoleRepository;
    private final UserInfraMapper mapper;

    public UserRepositoryImpl(
            JpaUserRepository jpaUserRepository,
            JpaRoleRepository jpaRoleRepository,
            UserInfraMapper mapper
    ) {
        this.jpaUserRepository = jpaUserRepository;
        this.jpaRoleRepository = jpaRoleRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<User> findById(Long userId) {
        return jpaUserRepository.findById(userId).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaUserRepository.findByEmail(email).map(mapper::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaUserRepository.existsByEmail(email);
    }

    @Override
    public User save(User user) {
        UserJpaEntity jpaEntity = mapper.toInfrastructure(user);

        // Gán role cho user (1 user = 1 role)
        if (user.getRole() != null) {
            RoleJpaEntity roleEntity = jpaRoleRepository.findByRoleName(user.getRole())
                    .orElseThrow(() -> new IllegalStateException("Role không tồn tại: " + user.getRole()));
            jpaEntity.setRoles(Set.of(roleEntity));
        }

        UserJpaEntity saved = jpaUserRepository.save(jpaEntity);
        return mapper.toDomain(saved);
    }

    @Override
    public void softDelete(Long userId) {
        jpaUserRepository.findById(userId).ifPresent(user -> {
            user.setIsDeleted(true);
            jpaUserRepository.save(user);
        });
    }

    @Override
    public void accumulatePaidAndRecalcRank(Long userId, BigDecimal amount) {
        jpaUserRepository.accumulatePaidAndRecalcRank(userId, amount);
    }

    @Override
    public User updateProfile(Long userId, String name, String email, String phone, String avatarUrl) {
        UserJpaEntity entity = jpaUserRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User không tồn tại: " + userId));
        if (name  != null) entity.setName(name);
        if (email != null) entity.setEmail(email);
        if (phone != null) entity.setPhone(phone);
        if (avatarUrl != null) entity.setAvatarUrl(avatarUrl);
        return mapper.toDomain(jpaUserRepository.save(entity));
    }
}


