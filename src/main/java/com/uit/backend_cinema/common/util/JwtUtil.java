package com.uit.backend_cinema.common.util;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import com.uit.backend_cinema.common.sercurity.CustomUserDetails;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtil {
    private static final String ACCESS = "ACCESS";
    private static final String RESET_PASSWORD = "RESET_PASSWORD";
    private static final String TICKET_QR = "TICKET_QR";

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private Long jwtExpiration;

    //Mã hóa secret key thành đối tượng key của thuật toán hmac sha
    private SecretKey getSigningKey() {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /** Access tokens only. Password reset and ticket QR use separate issuers. */
    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        if (extraClaims.containsKey("purpose") || extraClaims.containsKey("Purpose")) {
            throw new IllegalArgumentException("Token purpose cannot be supplied as an extra claim");
        }
        return generateUserToken(extraClaims, userDetails, ACCESS);
    }

    public String generateResetToken(UserDetails userDetails) {
        return generateUserToken(Map.of(), userDetails, RESET_PASSWORD);
    }

    private String generateUserToken(Map<String, Object> claims, UserDetails userDetails, String purpose) {
        if (!isAccountActive(userDetails)) {
            throw new IllegalArgumentException("Account is not active");
        }
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .claims(claims)
                .claim("purpose", purpose)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(now))
                .expiration(new Date(now + jwtExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    //Lấy tên cụ thể của user từ trong thông tin lấy được từ token
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey()) //Đưa secret key ra để đối chiếu
                .build()
                .parseSignedClaims(token) //Phân tách token
                .getPayload(); //Lấy thông tin ra
    }

    // No legacy-access fallback: clients must refresh or sign in again.
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final Claims claims = extractAllClaims(token);
        if (!hasPurpose(claims, ACCESS) || !hasActiveExpiration(claims)
                || !isAccountActive(userDetails)
                || claims.getSubject() == null || !claims.getSubject().equals(userDetails.getUsername())) {
            return false;
        }
        // Email alone must not let an old token authenticate a different account.
        return userDetails instanceof CustomUserDetails user
                && user.getUserId() != null && user.getUserId() > 0
                && user.getUserId().equals(claims.get("userId", Long.class));
    }

    public boolean isTokenActive(String token) {
        return hasActiveExpiration(extractAllClaims(token));
    }

    public boolean verifyResetToken(String token, String expectedEmail) {
        final Claims claims = extractAllClaims(token);
        return hasPurpose(claims, RESET_PASSWORD) && hasActiveExpiration(claims)
                && expectedEmail != null && !expectedEmail.isBlank()
                && expectedEmail.equals(claims.getSubject());
    }

    private boolean hasPurpose(Claims claims, String purpose) {
        // Reject ambiguous tokens using both the legacy and canonical claim names.
        return purpose.equals(claims.get("purpose")) && !claims.containsKey("Purpose");
    }

    private boolean hasActiveExpiration(Claims claims) {
        return claims.getExpiration() != null && claims.getExpiration().after(new Date());
    }

    private boolean isAccountActive(UserDetails user) {
        return user != null && user.getUsername() != null && !user.getUsername().isBlank()
                && user.isEnabled() && user.isAccountNonLocked()
                && user.isAccountNonExpired() && user.isCredentialsNonExpired();
    }

    public String generateTicketQrToken(Long ticketId, Long bookingId, Long showtimeId) {
        return Jwts.builder()
                .claim("ticketId", ticketId)
                .claim("bookingId", bookingId)
                .claim("showtimeId", showtimeId)
                .claim("purpose", TICKET_QR)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + 365L * 24 * 60 * 60 * 1000)) // 1 year expiration
                .signWith(getSigningKey())
                .compact();
    }

    public Claims validateTicketQrToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            if (!hasPurpose(claims, TICKET_QR) || !hasActiveExpiration(claims)) {
                return null;
            }
            return claims;
        } catch (Exception e) {
            return null;
        }
    }
}
