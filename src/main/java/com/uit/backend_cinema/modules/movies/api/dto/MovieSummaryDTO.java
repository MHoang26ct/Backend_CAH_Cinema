package com.uit.backend_cinema.modules.movies.api.dto;

import java.time.LocalDate;

import lombok.Data;

@Data
public class MovieSummaryDTO {
    private Long movieId;
    private String title;
    private Integer duration;
    private LocalDate releaseDate;
    private String ageRating;
    private String posterUrl;
}
