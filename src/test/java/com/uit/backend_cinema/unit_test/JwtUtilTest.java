package com.uit.backend_cinema.unit_test;

import java.util.List;
import java.util.Map;

import com.uit.backend_cinema.common.sercurity.CustomUserDetails;
import com.uit.backend_cinema.common.util.JwtUtil;
import com.uit.backend_cinema.modules.auth.api.mapper.UserApiMapper;
import com.uit.backend_cinema.modules.auth.application.AuthUseCase;
import com.uit.backend_cinema.modules.auth.domain.entity.RefreshToken;
import com.uit.backend_cinema.modules.auth.domain.entity.User;
import com.uit.backend_cinema.modules.auth.domain.service.AuthService;
import com.uit.backend_cinema.modules.auth.domain.service.RefreshTokenService;
import com.uit.backend_cinema.modules.auth.infrastructure.security.CustomUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtUtilTest {
    private static final String EMAIL = "customer@example.com";
    private JwtUtil jwt;
    private CustomUserDetails principal;

    @BeforeEach
    void setup() {
        jwt = new JwtUtil();
        ReflectionTestUtils.setField(jwt, "secretKey", "offline-test-key-only-at-least-32-bytes-long");
        ReflectionTestUtils.setField(jwt, "jwtExpiration", 60_000L);
        principal = new CustomUserDetails(7L, EMAIL, "encoded",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Test
    void accessTokenHasExplicitPurposeAndCannotResetPasswordOrCheckIn() {
        String token = jwt.generateToken(Map.of("userId", 7L), principal);
        assertEquals("ACCESS", jwt.extractClaim(token, claims -> claims.get("purpose")));
        assertTrue(jwt.isTokenValid(token, principal));
        assertFalse(jwt.verifyResetToken(token, EMAIL));
        assertNull(jwt.validateTicketQrToken(token));
    }

    @Test
    void resetTokenOnlyWorksForPasswordResetOfSameSubject() {
        String token = jwt.generateResetToken(principal);
        assertEquals("RESET_PASSWORD", jwt.extractClaim(token, claims -> claims.get("purpose")));
        assertTrue(jwt.verifyResetToken(token, EMAIL));
        assertFalse(jwt.verifyResetToken(token, "someone-else@example.com"));
        assertFalse(jwt.verifyResetToken(token, null));
        assertFalse(jwt.isTokenValid(token, principal));
        assertNull(jwt.validateTicketQrToken(token));
    }

    @Test
    void qrOnlyWorksForCheckIn() {
        String token = jwt.generateTicketQrToken(1L, 2L, 3L);
        var claims = jwt.validateTicketQrToken(token);
        assertNotNull(claims);
        assertEquals(1L, claims.get("ticketId", Long.class));
        assertEquals(2L, claims.get("bookingId", Long.class));
        assertEquals(3L, claims.get("showtimeId", Long.class));
        assertFalse(jwt.isTokenValid(token, principal));
        assertFalse(jwt.verifyResetToken(token, EMAIL));
    }

    @Test
    void accessIssuerRejectsCallerSuppliedPurpose() {
        assertThrows(IllegalArgumentException.class,
                () -> jwt.generateToken(Map.of("Purpose", "Change password"), principal));
        assertThrows(IllegalArgumentException.class,
                () -> jwt.generateToken(Map.of("purpose", "TICKET_QR"), principal));
    }

    @Test
    void accessTokenIsBoundToSubjectAsWellAsAccountId() {
        String token = jwt.generateToken(Map.of("userId", 7L), principal);
        var other = new CustomUserDetails(7L, "changed@example.com", "encoded", List.of());
        assertFalse(jwt.isTokenValid(token, other));
    }

    @Test
    void authUseCaseUsesDedicatedResetIssuerAndAccessIssuerForLoginAndRefresh() {
        var auth = mock(AuthService.class);
        var refresh = mock(RefreshTokenService.class);
        var details = mock(CustomUserDetailsService.class);
        var useCase = new AuthUseCase(auth, refresh, details, jwt, mock(UserApiMapper.class));
        when(details.loadUserByUsername(EMAIL)).thenReturn(principal);
        when(details.loadUserById(7L)).thenReturn(principal);
        User user = new User();
        user.setUserId(7L);
        when(auth.login(EMAIL, "password")).thenReturn(user);
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(7L);
        when(refresh.findByToken("refresh")).thenReturn(java.util.Optional.of(refreshToken));
        when(refresh.verifyExpiration(refreshToken)).thenReturn(refreshToken);

        assertTrue(jwt.verifyResetToken(useCase.generateResetToken(EMAIL).getToken(), EMAIL));
        assertTrue(jwt.isTokenValid(useCase.login(EMAIL, "password").getAccessToken(), principal));
        assertTrue(jwt.isTokenValid(useCase.refreshAccessToken("refresh").get("accessToken"), principal));
    }
}
