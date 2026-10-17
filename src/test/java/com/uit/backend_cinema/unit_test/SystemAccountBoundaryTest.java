package com.uit.backend_cinema.unit_test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.uit.backend_cinema.common.exception.BusinessException;
import com.uit.backend_cinema.common.sercurity.CustomUserDetails;
import com.uit.backend_cinema.common.sercurity.JwtAuthenticationFilter;
import com.uit.backend_cinema.common.util.JwtUtil;
import com.uit.backend_cinema.modules.auth.api.mapper.UserApiMapper;
import com.uit.backend_cinema.modules.auth.application.AuthUseCase;
import com.uit.backend_cinema.modules.auth.application.UserUseCase;
import com.uit.backend_cinema.modules.auth.domain.entity.AccountType;
import com.uit.backend_cinema.modules.auth.domain.entity.RefreshToken;
import com.uit.backend_cinema.modules.auth.domain.entity.User;
import com.uit.backend_cinema.modules.auth.domain.repository.RefreshTokenRepository;
import com.uit.backend_cinema.modules.auth.domain.repository.UserRepository;
import com.uit.backend_cinema.modules.auth.domain.service.AuthService;
import com.uit.backend_cinema.modules.auth.domain.service.RefreshTokenService;
import com.uit.backend_cinema.modules.auth.infrastructure.mapper.UserInfraMapper;
import com.uit.backend_cinema.modules.auth.infrastructure.security.CustomUserDetailsService;
import com.uit.backend_cinema.modules.booking.infrastructure.repository.JpaBookingRepository;
import com.uit.backend_cinema.modules.notification.domain.service.NotificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mapstruct.factory.Mappers;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SystemAccountBoundaryTest {
    private User user;
    private UserRepository users;
    private PasswordEncoder passwords;
    private AuthService auth;
    private RefreshTokenRepository tokenStore;
    private RefreshTokenService refresh;
    private AuthUseCase authUseCase;
    private CustomUserDetailsService details;
    private JwtUtil jwt;

    @BeforeEach
    void setup() {
        SecurityContextHolder.clearContext();
        user = new User();
        user.setUserId(7L);
        user.setAccountType(AccountType.WALK_IN);
        // Prove that even an accidentally assigned email/password/role grants no access.
        user.setEmail("system@example.com");
        user.setPassword("encoded");
        user.setRole("ROLE_ADMIN");
        users = mock(UserRepository.class);
        when(users.findById(7L)).thenReturn(Optional.of(user));
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        passwords = mock(PasswordEncoder.class);
        auth = new AuthService(users, passwords, mock(NotificationService.class));
        tokenStore = mock(RefreshTokenRepository.class);
        refresh = new RefreshTokenService(tokenStore, users);
        details = new CustomUserDetailsService(users);
        jwt = new JwtUtil();
        ReflectionTestUtils.setField(jwt, "secretKey", "offline-test-key-only-at-least-32-bytes-long");
        ReflectionTestUtils.setField(jwt, "jwtExpiration", 60_000L);
        authUseCase = new AuthUseCase(auth, refresh, details, jwt, mock(UserApiMapper.class));
    }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest
    @ValueSource(strings = {"WALK_IN", "DELETED", "UNKNOWN"})
    void rejectsNonInteractiveAccountBeforePasswordCheck(String reason) {
        if (reason.equals("DELETED")) {
            user.setAccountType(AccountType.REGISTERED);
            user.setIsDeleted(true);
        } else if (reason.equals("UNKNOWN")) {
            user.setAccountType(null);
        }
        assertThrows(BusinessException.class, () -> auth.login(user.getEmail(), "password"));
        verifyNoInteractions(passwords);
        verify(users, never()).save(any());
    }

    @Test
    void cannotLinkGoogleOrChangeSystemProfileThroughGoogleLogin() {
        assertThrows(BusinessException.class,
                () -> auth.loginOrRegisterWithGoogle(user.getEmail(), "New name", "avatar", "google-subject"));
        assertNull(user.getProviderId());
        assertNull(user.getName());
        verify(users, never()).save(any());
    }

    @Test
    void cannotIssueResetTokenOrChangeSystemPassword() {
        assertThrows(UsernameNotFoundException.class, () -> authUseCase.generateResetToken(user.getEmail()));
        assertThrows(BusinessException.class, () -> auth.resetPassword(user.getEmail(), "new-password"));
        assertThrows(BusinessException.class,
                () -> auth.changePasswordWithOldPassword(user.getEmail(), "old-password", "new-password"));
        assertEquals("encoded", user.getPassword());
        verifyNoInteractions(passwords);
        verify(users, never()).save(any());
    }

    @Test
    void cannotCreateOrUseRefreshTokenForSystemAccount() {
        assertThrows(BusinessException.class, () -> refresh.createRefreshToken(7L));
        verify(tokenStore, never()).save(any());
        RefreshToken old = new RefreshToken();
        old.setUserId(7L);
        old.setExpiryDate(LocalDateTime.now().plusDays(1));
        when(tokenStore.findByToken("old-refresh")).thenReturn(Optional.of(old));
        assertThrows(UsernameNotFoundException.class, () -> authUseCase.refreshAccessToken("old-refresh"));
    }

    @Test
    void cannotReadSharedHistoryOrUpdateProfile() {
        var bookings = mock(JpaBookingRepository.class);
        var profiles = new UserUseCase(users, bookings, mock(UserApiMapper.class));
        assertThrows(BusinessException.class, () -> profiles.getProfile(7L));
        assertThrows(BusinessException.class,
                () -> profiles.updateProfile(7L, "Other", "other@example.com", null, null));
        verifyNoInteractions(bookings);
        verify(users, never()).updateProfile(any(), any(), any(), any(), any());
    }

    @Test
    void previouslySignedAccessTokenStillCannotAuthenticateSystemAccount() throws Exception {
        String token = jwt.generateToken(Map.of("userId", 7L),
                new CustomUserDetails(7L, user.getEmail(), "encoded", List.of()));
        var filter = new JwtAuthenticationFilter(jwt, details);
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        var chain = mock(jakarta.servlet.FilterChain.class);
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, chain);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
    }

    @Test
    void accountTypeRoundTripsThroughPersistenceMapper() {
        var mapper = Mappers.getMapper(UserInfraMapper.class);
        var entity = mapper.toInfrastructure(user);
        assertEquals(AccountType.WALK_IN, entity.getAccountType());
        assertEquals(AccountType.WALK_IN, mapper.toDomain(entity).getAccountType());
        assertFalse(mapper.toDomain(entity).isInteractiveAccount());
    }

    @Test
    void registeredAccountsStillAuthenticateAndReceiveRefreshTokens() {
        user.setAccountType(AccountType.REGISTERED);
        when(passwords.matches("password", "encoded")).thenReturn(true);
        assertSame(user, auth.login(user.getEmail(), "password"));
        assertEquals(7L, ((CustomUserDetails) details.loadUserById(7L)).getUserId());
        assertNotNull(refresh.createRefreshToken(7L));
        verify(tokenStore).save(any(RefreshToken.class));
    }
}
