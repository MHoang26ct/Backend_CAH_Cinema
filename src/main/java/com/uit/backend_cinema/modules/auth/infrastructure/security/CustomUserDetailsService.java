package com.uit.backend_cinema.modules.auth.infrastructure.security;

import java.util.Collections;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.uit.backend_cinema.common.sercurity.CustomUserDetails;
import com.uit.backend_cinema.modules.auth.domain.entity.User;
import com.uit.backend_cinema.modules.auth.domain.repository.UserRepository;

/**
 * Adapter triển khai UserDetailsService của Spring Security.
 * Thuộc infrastructure vì nó phụ thuộc trực tiếp vào Spring Security API —
 * không phải domain logic.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Load user bằng email (dùng cho JWT authentication filter)
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // UserRepository.findByEmail() đã tự lọc isDeleted=true → không cần filter lại
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy user với email: " + email));

        return buildUserDetails(user);
    }

    // Load user bằng userId (dùng cho refresh token flow)
    public UserDetails loadUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy user với id: " + userId));

        return buildUserDetails(user);
    }

    private UserDetails buildUserDetails(User user) {
        String username = user.getEmail() != null ? user.getEmail() : user.getPhone();
        String roleName = user.getRole() != null ? user.getRole() : "ROLE_USER";

        return new CustomUserDetails(
                user.getUserId(),
                username,
                user.getPassword() != null ? user.getPassword() : "",
                Collections.singletonList(new SimpleGrantedAuthority(roleName))
        );
    }
}
