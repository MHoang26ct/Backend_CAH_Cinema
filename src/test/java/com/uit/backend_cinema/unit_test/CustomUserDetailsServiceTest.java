package com.uit.backend_cinema.unit_test;

import java.util.Optional;

import com.uit.backend_cinema.common.sercurity.CustomUserDetails;
import com.uit.backend_cinema.modules.auth.domain.entity.User;
import com.uit.backend_cinema.modules.auth.domain.repository.UserRepository;
import com.uit.backend_cinema.modules.auth.infrastructure.security.CustomUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CustomUserDetailsServiceTest {
    private UserRepository users;
    private CustomUserDetailsService service;
    private User user;

    @BeforeEach
    void setup() {
        users = mock(UserRepository.class);
        service = new CustomUserDetailsService(users);
        user = new User();
        user.setUserId(7L);
        user.setEmail("customer@example.com");
        user.setRole("ROLE_USER");
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(users.findById(7L)).thenReturn(Optional.of(user));
    }

    @Test
    void loadsActiveAccountByEmailAndId() {
        assertEquals(7L, ((CustomUserDetails) service.loadUserByUsername(user.getEmail())).getUserId());
        assertEquals(user.getEmail(), service.loadUserById(7L).getUsername());
    }

    @Test
    void rejectsDeletedAccountEvenIfRepositoryReturnsIt() {
        user.setIsDeleted(true);
        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername(user.getEmail()));
        assertThrows(UsernameNotFoundException.class, () -> service.loadUserById(7L));
    }

    @Test
    void rejectsMissingAccountForAccessAndRefresh() {
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.empty());
        when(users.findById(7L)).thenReturn(Optional.empty());
        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername(user.getEmail()));
        assertThrows(UsernameNotFoundException.class, () -> service.loadUserById(7L));
    }

    @Test
    void rejectsAccountWithoutPersistentIdentity() {
        user.setUserId(null);
        assertThrows(UsernameNotFoundException.class, () -> service.loadUserById(7L));
    }
}
