package com.uit.backend_cinema.modules.movies.api.dto;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.FutureOrPresent;

import lombok.Data;

@Data
public class UpdateOrCreateMovieDTO {
    public interface Create extends jakarta.validation.groups.Default {}
    private String title;
    private String description;

    @jakarta.validation.constraints.NotNull(message = "Thời lượng phim không được trống")
    @Min(value = 15, message = "Thời lượng phim phải từ 15 phút")
    private Integer duration;

    @FutureOrPresent(groups = Create.class, message = "Ngày phát hành không được trước hôm nay")
    private LocalDate releaseDate;

    private String ageRating;
    private String posterUrl;
    private String trailerUrl;
    private String directorName;
    private String actorList;

    @NotEmpty(message = "Phải có ít nhất 1 thể loại")
    private List<Long> genreIdList;
}
