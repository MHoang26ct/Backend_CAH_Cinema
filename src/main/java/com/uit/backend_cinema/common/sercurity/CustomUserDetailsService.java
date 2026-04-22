package com.uit.backend_cinema.common.sercurity;

import com.uit.backend_cinema.modules.auth.infrastructure.entity.RoleJpaEntity;
import com.uit.backend_cinema.modules.auth.infrastructure.entity.UserJpaEntity;
import com.uit.backend_cinema.modules.auth.infrastructure.repository.JpaUserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final JpaUserRepository userRepository;

    public CustomUserDetailsService(JpaUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Load user bằng email (dùng cho JWT authentication filter)
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        UserJpaEntity user = userRepository.findByEmail(email)
                .filter(u -> !Boolean.TRUE.equals(u.getIsDeleted()))
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy user với email: " + email));

        return buildUserDetails(user);
    }

    // Load user bằng userId (dùng cho refresh token flow)
    public UserDetails loadUserById(Long userId) {
        UserJpaEntity user = userRepository.findById(userId)
                .filter(u -> !Boolean.TRUE.equals(u.getIsDeleted()))
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy user với id: " + userId));

        return buildUserDetails(user);
    }

    private UserDetails buildUserDetails(UserJpaEntity user) {
        String username = user.getEmail() != null ? user.getEmail() : user.getPhone();

        // Lấy role duy nhất của user từ bảng user_roles → roles
        String roleName = user.getRoles().stream()
                .findFirst()
                .map(RoleJpaEntity::getRoleName)
                .orElse("ROLE_USER"); // Fallback nếu chưa gán role

        return new User(
                username,
                user.getPassword() != null ? user.getPassword() : "",
                Collections.singletonList(new SimpleGrantedAuthority(roleName))
        );
    }
}
