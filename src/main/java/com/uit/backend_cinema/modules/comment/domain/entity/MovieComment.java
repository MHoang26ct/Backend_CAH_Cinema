package com.uit.backend_cinema.modules.comment.domain.entity;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MovieComment {
    private Long commentId;
    private Long movieId;
    private Long userId;
    private String userName;
    private String userAvatar;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isDeleted = false;
}
