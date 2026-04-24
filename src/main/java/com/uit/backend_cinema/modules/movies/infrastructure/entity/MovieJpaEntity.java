package com.uit.backend_cinema.modules.movies.infrastructure.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "movies")
@SQLRestriction("is_deleted = false")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class MovieJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "movie_id")
    private Long movieId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "duration", nullable = false)
    private Integer duration;

    @Column(name = "release_date")
    private LocalDate releaseDate;

    @Column(name = "age_rating", nullable = false)
    private String ageRating;

    @Column(name = "poster_url", columnDefinition = "TEXT")
    private String posterUrl;

    @Column(name = "trailer_url", columnDefinition = "TEXT")
    private String trailerUrl;

    @Column(name = "director_name")
    private String directorName;

    @Column(name = "actor_list", columnDefinition = "TEXT")
    private String actorList;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "movie_genres", joinColumns = @JoinColumn(name = "movie_id"), inverseJoinColumns = @JoinColumn(name = "genre_id"))
    private Set<GenreJpaEntity> genres = new HashSet<>();

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "is_deleted")
    private Boolean isDeleted = false;
}
