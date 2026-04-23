package com.uit.backend_cinema.modules.user.movies.api.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class MovieSummaryDTO {
    private Long movieId;
    private String title;
    private Integer duration;
    private LocalDate releaseDate;
    private String ageRating;
    private String posterUrl;
}
