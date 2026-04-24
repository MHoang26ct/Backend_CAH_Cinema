package com.uit.backend_cinema.modules.movies.domain.entity;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter @Setter
public class Genre {
    private Long genreId;
    private String name;
    private LocalDateTime createdAt;
    private Boolean isDeleted;
}
