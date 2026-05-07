package com.uit.backend_cinema.modules.auth.infrastructure.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.uit.backend_cinema.modules.auth.domain.entity.RefreshToken;
import com.uit.backend_cinema.modules.auth.infrastructure.entity.RefreshTokenJpaEntity;

@Mapper(componentModel = "spring")
public interface RefreshTokenInfraMapper {

    @Mapping(target = "user", ignore = true) // User entity sẽ được set thủ công trong RepositoryImpl
    RefreshTokenJpaEntity toInfrastructure(RefreshToken refreshToken);

    @Mapping(source = "user.userId", target = "userId")
    RefreshToken toDomain(RefreshTokenJpaEntity refreshTokenJpaEntity);
}
