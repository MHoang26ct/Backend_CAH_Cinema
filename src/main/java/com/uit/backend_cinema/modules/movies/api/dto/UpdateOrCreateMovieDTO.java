package com.uit.backend_cinema.modules.movies.api.dto;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;

import lombok.Data;

@Data
public class UpdateOrCreateMovieDTO {
    private String title;
    private String description;

    @Min(value = 15, message = "Độ dài phim phải lớn hơn 15 phút")
    private Integer duration;

    @FutureOrPresent(message = "Ngày phát hành phải lớn hơn hiện tại")
    private LocalDate releaseDate;

    private String ageRating;
    private String posterUrl;
    private String trailerUrl;
    private String directorName;
    private String actorList;

    @NotEmpty(message = "Phải có ít nhất 1 thể loại")
    private List<Long> genreIdList;
}
