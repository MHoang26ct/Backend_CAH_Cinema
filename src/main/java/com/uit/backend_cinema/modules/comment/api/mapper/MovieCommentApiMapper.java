package com.uit.backend_cinema.modules.comment.api.mapper;

import org.mapstruct.Mapper;

import com.uit.backend_cinema.modules.comment.domain.entity.MovieComment;
import com.uit.backend_cinema.modules.comment.api.dto.response.CommentResponse;

@Mapper(componentModel = "spring")
public interface MovieCommentApiMapper {
    CommentResponse toResponse(MovieComment comment);
}
