package com.uit.backend_cinema.modules.movies.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.uit.backend_cinema.modules.movies.infrastructure.entity.GenreJpaEntity;

@Repository
public interface JpaGenreRepository extends JpaRepository<GenreJpaEntity, Long> {
}
