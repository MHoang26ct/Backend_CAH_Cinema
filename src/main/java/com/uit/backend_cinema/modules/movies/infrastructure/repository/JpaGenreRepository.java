package com.uit.backend_cinema.modules.movies.infrastructure.repository;

import com.uit.backend_cinema.modules.movies.infrastructure.entity.GenreJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaGenreRepository extends JpaRepository<GenreJpaEntity, Long> {
}
