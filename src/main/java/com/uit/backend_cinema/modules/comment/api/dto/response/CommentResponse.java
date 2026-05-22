package com.uit.backend_cinema.modules.comment.api.dto.response;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class CommentResponse {
    private Long commentId;
    private Long userId;
    private String userName;
    private String userAvatar;
    private String content;
    private LocalDateTime createdAt;
}
