package com.uit.backend_cinema.modules.movies.api.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class MovieDetailDTO {
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
    private List<GenreDTO> genres;
}
