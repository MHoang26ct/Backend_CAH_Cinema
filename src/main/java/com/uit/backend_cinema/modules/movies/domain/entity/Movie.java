package com.uit.backend_cinema.modules.movies.domain.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class Movie {
    private Long movieId;
    private String title;
    private String description;
    private Integer duration;
    private LocalDate releaseDate;
    private AgeRating ageRating;
    private String posterUrl;
    private String trailerUrl;
    private String directorName;
    private String actorList;
    private List<Genre> genres;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isDeleted;
}
