package com.uit.backend_cinema.modules.comment.infrastructure.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.uit.backend_cinema.modules.comment.domain.entity.MovieComment;
import com.uit.backend_cinema.modules.comment.infrastructure.entity.MovieCommentJpaEntity;

@Mapper(componentModel = "spring")
public interface MovieCommentInfraMapper {

    @Mapping(target = "userName", source = "user.name")
    @Mapping(target = "userAvatar", source = "user.avatarUrl")
    MovieComment toDomain(MovieCommentJpaEntity entity);

    @Mapping(target = "user", ignore = true)
    @Mapping(target = "movie", ignore = true)
    MovieCommentJpaEntity toInfrastructure(MovieComment comment);
}
