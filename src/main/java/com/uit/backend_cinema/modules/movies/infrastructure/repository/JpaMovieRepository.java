package com.uit.backend_cinema.modules.movies.infrastructure.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.uit.backend_cinema.modules.movies.infrastructure.entity.MovieJpaEntity;

@Repository
public interface JpaMovieRepository extends JpaRepository<MovieJpaEntity, Long> {
    
    @Query(value = """
        SELECT DISTINCT m.* FROM movies m
        LEFT JOIN movie_genres mg ON m.movie_id = mg.movie_id
        LEFT JOIN genres g ON mg.genre_id = g.genre_id AND g.is_deleted = false
        WHERE m.is_deleted = false
        AND (CAST(:title AS text) IS NULL OR LOWER(m.title) LIKE LOWER(CONCAT('%', CAST(:title AS text), '%')))
        AND (CAST(:genreId AS bigint) IS NULL OR mg.genre_id = :genreId)
        AND (CAST(:ageRating AS text) IS NULL OR m.age_rating = CAST(:ageRating AS text))
    """, countQuery = """
        SELECT COUNT(DISTINCT m.movie_id) FROM movies m
        LEFT JOIN movie_genres mg ON m.movie_id = mg.movie_id
        LEFT JOIN genres g ON mg.genre_id = g.genre_id AND g.is_deleted = false
        WHERE m.is_deleted = false
        AND (CAST(:title AS text) IS NULL OR LOWER(m.title) LIKE LOWER(CONCAT('%', CAST(:title AS text), '%')))
        AND (CAST(:genreId AS bigint) IS NULL OR mg.genre_id = :genreId)
        AND (CAST(:ageRating AS text) IS NULL OR m.age_rating = CAST(:ageRating AS text))
    """, nativeQuery = true)
    Page<MovieJpaEntity> searchMovies(
        @Param("title") String title,
        @Param("genreId") Long genreId,
        @Param("ageRating") String ageRating,
        Pageable pageable
    );

    boolean existsByTitleAndReleaseDateAfter(String title, LocalDate releaseDate);

    @Query(value = """
        SELECT DISTINCT m.* FROM movies m
        WHERE m.is_deleted = false
        AND m.release_date <= CURRENT_DATE
        ORDER BY m.release_date DESC
        LIMIT 5
    """, nativeQuery = true)
    List<MovieJpaEntity> findNowShowing();

    @Query(value = """
        SELECT DISTINCT m.* FROM movies m
        WHERE m.is_deleted = false
        AND m.release_date > CURRENT_DATE
        ORDER BY m.release_date ASC
        LIMIT 5
    """, nativeQuery = true)
    List<MovieJpaEntity> findUpcoming();
}
