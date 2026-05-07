package com.uit.backend_cinema.modules.auth.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.uit.backend_cinema.modules.auth.api.dto.UserDTO;
import com.uit.backend_cinema.modules.auth.domain.entity.User;

@Mapper(componentModel = "spring")
public interface UserApiMapper {

    @Mapping(source = "authProvider", target = "authProvider")
    UserDTO toDto(User user);
}
