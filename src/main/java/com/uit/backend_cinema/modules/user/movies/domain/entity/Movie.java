package com.uit.backend_cinema.modules.user.movies.domain.entity;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter
public class Movie {
    private Long movieId;
    private String title;
    private String description;
    private Integer duration;
    private LocalDate releaseDate;
    private String ageRating;
    private String posterUrl;
    private String trailerUrl;
    private String directorName;
    private String actorList;
    private List<Genre> genres;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isDeleted;
}
