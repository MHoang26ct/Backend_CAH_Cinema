package com.uit.backend_cinema.unit_test;

import com.uit.backend_cinema.modules.auth.domain.entity.AuthProvider;
import com.uit.backend_cinema.modules.auth.domain.entity.User;
import com.uit.backend_cinema.modules.auth.domain.repository.UserRepository;
import com.uit.backend_cinema.modules.auth.domain.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthModuleServiceTest {

    @Test
    @DisplayName("Auth module: register mã hóa mật khẩu và gán ROLE_USER")
    void registerEncodesPasswordAndAssignsDefaultRole() {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        AuthService authService = new AuthService(userRepository, passwordEncoder);

        when(userRepository.existsByEmail("user@cah.vn")).thenReturn(false);
        when(passwordEncoder.encode("secret")).thenReturn("encoded-secret");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User registered = authService.register("user@cah.vn", "secret", "Cinema User", "0900000000");

        assertEquals("encoded-secret", registered.getPassword());
        assertEquals("ROLE_USER", registered.getRole());
        assertEquals(AuthProvider.EMAIL, registered.getAuthProvider());
        assertFalse(registered.getIsDeleted());
    }
}
