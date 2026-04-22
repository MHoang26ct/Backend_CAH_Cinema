package com.uit.backend_cinema.modules.auth.infrastructure.mapper;

import com.uit.backend_cinema.modules.auth.domain.entity.AuthProvider;
import com.uit.backend_cinema.modules.auth.domain.entity.User;
import com.uit.backend_cinema.modules.auth.infrastructure.entity.RoleJpaEntity;
import com.uit.backend_cinema.modules.auth.infrastructure.entity.UserJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Set;

@Mapper(componentModel = "spring")
public interface UserInfraMapper {

    @Mapping(source = "authProvider", target = "authProvider", qualifiedByName = "enumToString")
    @Mapping(target = "roles", ignore = true) // Role sẽ được set thủ công trong RepositoryImpl
    UserJpaEntity toInfrastructure(User user);

    @Mapping(source = "authProvider", target = "authProvider", qualifiedByName = "stringToEnum")
    @Mapping(source = "roles", target = "role", qualifiedByName = "rolesToSingleString")
    User toDomain(UserJpaEntity userJpaEntity);

    @Named("enumToString")
    default String enumToString(AuthProvider authProvider) {
        return authProvider != null ? authProvider.name() : null;
    }

    @Named("stringToEnum")
    default AuthProvider stringToEnum(String authProvider) {
        return authProvider != null ? AuthProvider.valueOf(authProvider) : null;
    }

    // Lấy role đầu tiên (và duy nhất) từ Set<RoleJpaEntity> → String
    @Named("rolesToSingleString")
    default String rolesToSingleString(Set<RoleJpaEntity> roles) {
        if (roles == null || roles.isEmpty()) {
            return null;
        }
        return roles.iterator().next().getRoleName();
    }
}
