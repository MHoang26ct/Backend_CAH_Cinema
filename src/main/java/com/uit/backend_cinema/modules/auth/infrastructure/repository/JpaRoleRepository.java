package com.uit.backend_cinema.modules.auth.infrastructure.repository;

import com.uit.backend_cinema.modules.auth.infrastructure.entity.RoleJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JpaRoleRepository extends JpaRepository<RoleJpaEntity, Integer> {
    Optional<RoleJpaEntity> findByRoleName(String roleName);
}
