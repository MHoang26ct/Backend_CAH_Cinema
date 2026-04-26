package com.uit.backend_cinema.modules.movies.infrastructure.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.uit.backend_cinema.modules.movies.infrastructure.entity.MovieJpaEntity;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface JpaMovieRepository extends JpaRepository<MovieJpaEntity, Long> {
    
    @Query("""
        SELECT DISTINCT m FROM MovieJpaEntity m
        LEFT JOIN m.genres g
        WHERE (:title IS NULL OR LOWER(m.title) LIKE LOWER(CONCAT('%', :title, '%')))
        AND (:genreId IS NULL OR g.genreId = :genreId)
        AND (:ageRating IS NULL OR m.ageRating = :ageRating)
    """)
    Page<MovieJpaEntity> searchMovies(
        @Param("title") String title,
        @Param("genreId") Long genreId,
        @Param("ageRating") String ageRating,
        Pageable pageable
    );

    boolean existsByTitleAndReleaseDateAfter(String title, LocalDate releaseDate);
}
