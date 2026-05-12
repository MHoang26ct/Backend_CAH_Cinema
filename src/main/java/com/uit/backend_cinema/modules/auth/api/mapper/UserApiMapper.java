package com.uit.backend_cinema.modules.auth.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import com.uit.backend_cinema.modules.auth.api.dto.UserDTO;
import com.uit.backend_cinema.modules.auth.domain.entity.User;
import com.uit.backend_cinema.modules.auth.domain.entity.UserRank;

@Mapper(componentModel = "spring")
public interface UserApiMapper {

    @Mapping(source = "authProvider", target = "authProvider")
    @Mapping(source = "rankLevel", target = "rankLevel", qualifiedByName = "rankToString")
    UserDTO toDto(User user);

    @Named("rankToString")
    default String rankToString(UserRank rank) {
        return rank != null ? rank.name() : UserRank.SILVER.name();
    }
}

