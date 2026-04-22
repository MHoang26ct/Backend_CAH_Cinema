package com.uit.backend_cinema.modules.auth.infrastructure.persistence;

import com.uit.backend_cinema.modules.auth.domain.entity.User;
import com.uit.backend_cinema.modules.auth.domain.repository.UserRepository;
import com.uit.backend_cinema.modules.auth.infrastructure.entity.RoleJpaEntity;
import com.uit.backend_cinema.modules.auth.infrastructure.entity.UserJpaEntity;
import com.uit.backend_cinema.modules.auth.infrastructure.mapper.UserInfraMapper;
import com.uit.backend_cinema.modules.auth.infrastructure.repository.JpaRoleRepository;
import com.uit.backend_cinema.modules.auth.infrastructure.repository.JpaUserRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

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
        return jpaUserRepository.findById(userId)
                .filter(user -> !Boolean.TRUE.equals(user.getIsDeleted()))
                .map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaUserRepository.findByEmail(email)
                .filter(user -> !Boolean.TRUE.equals(user.getIsDeleted()))
                .map(mapper::toDomain);
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
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy role: " + user.getRole()));
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
}
