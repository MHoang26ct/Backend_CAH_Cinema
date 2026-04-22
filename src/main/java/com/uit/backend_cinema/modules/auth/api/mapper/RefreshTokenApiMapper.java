package com.uit.backend_cinema.modules.auth.api.mapper;

import com.uit.backend_cinema.modules.auth.api.dto.RefreshTokenDTO;
import com.uit.backend_cinema.modules.auth.domain.entity.RefreshToken;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RefreshTokenApiMapper {
    @Mapping(source = "refreshToken", target = "token")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "expiryDate", ignore = true)
    @Mapping(target = "userId", ignore = true)
    RefreshToken toDomain(RefreshTokenDTO refreshTokenDTO);

    @Mapping(source = "token", target = "refreshToken")
    RefreshTokenDTO toApi(RefreshToken refreshToken);
}
