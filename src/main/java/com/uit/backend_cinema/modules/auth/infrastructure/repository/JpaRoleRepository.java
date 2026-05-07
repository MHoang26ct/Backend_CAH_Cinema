package com.uit.backend_cinema.modules.auth.infrastructure.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.uit.backend_cinema.modules.auth.infrastructure.entity.RoleJpaEntity;

@Repository
public interface JpaRoleRepository extends JpaRepository<RoleJpaEntity, Integer> {
    Optional<RoleJpaEntity> findByRoleName(String roleName);
}
