package com.uit.backend_cinema.modules.user.movies.infrastructure.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "genres")
@Getter @Setter

public class GenreJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "genre_id")
    private Long genreId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "is_deleted")
    private Boolean isDeleted;
}
