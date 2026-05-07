package com.uit.backend_cinema.modules.movies.domain.entity;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class Genre {
    private Long genreId;
    private String name;
    private LocalDateTime createdAt;
    private Boolean isDeleted;
}
