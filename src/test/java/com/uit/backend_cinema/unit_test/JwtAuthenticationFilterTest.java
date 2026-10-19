package com.uit.backend_cinema.unit_test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.uit.backend_cinema.common.sercurity.CustomUserDetails;
import com.uit.backend_cinema.common.sercurity.JwtAuthenticationFilter;
import com.uit.backend_cinema.common.util.JwtUtil;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {
    private static final String SECRET = "offline-test-key-only-at-least-32-bytes-long";
    private static final String EMAIL = "customer@example.com";
    private JwtUtil jwt;
    private UserDetailsService users;
    private JwtAuthenticationFilter filter;
    private UserDetails principal;

    @BeforeEach
    void setup() {
        SecurityContextHolder.clearContext();
        jwt = new JwtUtil();
        ReflectionTestUtils.setField(jwt, "secretKey", SECRET);
        ReflectionTestUtils.setField(jwt, "jwtExpiration", 60_000L);
        users = mock(UserDetailsService.class);
        principal = new CustomUserDetails(7L, EMAIL, "encoded",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        when(users.loadUserByUsername(EMAIL)).thenReturn(principal);
        filter = new JwtAuthenticationFilter(jwt, users);
    }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatesAccessTokenUsingCurrentDatabaseAuthorities() throws Exception {
        authenticate(signed(Map.of("purpose", "ACCESS", "userId", 7L, "role", "ROLE_ADMIN")));
        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertSame(principal, auth.getPrincipal());
        assertEquals(principal.getAuthorities(), auth.getAuthorities());
    }

    @Test
    void legacyPasswordResetTokenCannotAuthenticateAsAccess() throws Exception {
        authenticate(signed(Map.of("Purpose", "Change password")));
        assertAnonymous();
    }

    @ParameterizedTest
    @ValueSource(strings = {"RESET_PASSWORD", "TICKET_QR", "unknown", "access", ""})
    void rejectsOtherPurposesEvenWithMatchingSubject(String purpose) throws Exception {
        authenticate(signed(Map.of("purpose", purpose, "userId", 7L)));
        assertAnonymous();
    }

    @Test
    void rejectsLegacyAccessWithoutPurpose() throws Exception {
        authenticate(signed(Map.of("userId", 7L)));
        assertAnonymous();
    }

    @Test
    void rejectsConflictingLegacyAndNewPurpose() throws Exception {
        authenticate(signed(Map.of("purpose", "ACCESS", "Purpose", "Change password", "userId", 7L)));
        assertAnonymous();
    }

    @Test
    void rejectsAccountIdMismatchEvenWhenEmailHasBeenReused() throws Exception {
        authenticate(signed(Map.of("purpose", "ACCESS", "userId", 99L)));
        assertAnonymous();
    }

    @Test
    void rejectsMissingAccountId() throws Exception {
        authenticate(signed(Map.of("purpose", "ACCESS")));
        assertAnonymous();
    }

    @ParameterizedTest
    @ValueSource(strings = {"disabled", "locked", "expired", "credentialsExpired"})
    void rejectsInactiveAccounts(String flag) throws Exception {
        var inactivePrincipal = spy(principal);
        switch (flag) {
            case "disabled" -> when(inactivePrincipal.isEnabled()).thenReturn(false);
            case "locked" -> when(inactivePrincipal.isAccountNonLocked()).thenReturn(false);
            case "expired" -> when(inactivePrincipal.isAccountNonExpired()).thenReturn(false);
            case "credentialsExpired" -> when(inactivePrincipal.isCredentialsNonExpired()).thenReturn(false);
            default -> throw new AssertionError(flag);
        }
        when(users.loadUserByUsername(EMAIL)).thenReturn(inactivePrincipal);
        authenticate(signed(Map.of("purpose", "ACCESS", "userId", 7L)));
        assertAnonymous();
    }

    @Test
    void rejectsDeletedOrMissingAccount() throws Exception {
        when(users.loadUserByUsername(EMAIL)).thenThrow(new UsernameNotFoundException("Not found"));
        authenticate(signed(Map.of("purpose", "ACCESS", "userId", 7L)));
        assertAnonymous();
    }

    @Test
    void rejectsExpiredToken() throws Exception {
        authenticate(Jwts.builder().subject(EMAIL).claim("purpose", "ACCESS").claim("userId", 7L)
                .expiration(Date.from(Instant.now().minusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact());
        assertAnonymous();
    }

    @Test
    void rejectsTokenWithoutExpiration() throws Exception {
        authenticate(Jwts.builder().subject(EMAIL).claim("purpose", "ACCESS").claim("userId", 7L)
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact());
        assertAnonymous();
    }

    @Test
    void rejectsInvalidSignature() throws Exception {
        authenticate(Jwts.builder().subject(EMAIL).claim("purpose", "ACCESS").claim("userId", 7L)
                .expiration(Date.from(Instant.now().plusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor((SECRET + "another-key").getBytes(StandardCharsets.UTF_8))).compact());
        assertAnonymous();
    }

    @Test
    void rejectsQrWithoutSubjectAndMalformedToken() throws Exception {
        authenticate(jwt.generateTicketQrToken(1L, 2L, 3L));
        assertAnonymous();
        authenticate("not-a-jwt");
        assertAnonymous();
    }

    private String signed(Map<String, Object> claims) {
        return Jwts.builder().claims(claims).subject(EMAIL)
                .expiration(Date.from(Instant.now().plusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
    }

    private void authenticate(String token) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        var chain = mock(jakarta.servlet.FilterChain.class);
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
    }

    private void assertAnonymous() {
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
