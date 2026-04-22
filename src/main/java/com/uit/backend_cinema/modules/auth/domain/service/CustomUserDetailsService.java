package com.uit.backend_cinema.modules.auth.domain.service;

import com.uit.backend_cinema.modules.auth.domain.entity.User;
import com.uit.backend_cinema.modules.auth.domain.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Load user bằng email (dùng cho JWT authentication filter)
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .filter(u -> !Boolean.TRUE.equals(u.getIsDeleted()))
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy user với email: " + email));

        return buildUserDetails(user);
    }

    // Load user bằng userId (dùng cho refresh token flow)
    public UserDetails loadUserById(Long userId) {
        User user = userRepository.findById(userId)
                .filter(u -> !Boolean.TRUE.equals(u.getIsDeleted()))
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy user với id: " + userId));

        return buildUserDetails(user);
    }

    private UserDetails buildUserDetails(User user) {
        String username = user.getEmail() != null ? user.getEmail() : user.getPhone();

        // Trong entity User, role đã được map thành 1 chuỗi (VD: "ROLE_USER")
        String roleName = user.getRole() != null ? user.getRole() : "ROLE_USER";

        return new org.springframework.security.core.userdetails.User(
                username,
                user.getPassword() != null ? user.getPassword() : "",
                Collections.singletonList(new SimpleGrantedAuthority(roleName))
        );
    }
}
