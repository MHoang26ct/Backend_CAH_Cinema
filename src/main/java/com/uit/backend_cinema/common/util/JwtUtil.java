package com.uit.backend_cinema.common.util;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {
    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private Long jwtExpiration;

    //Mã hóa secret key thành đối tượng key của thuật toán hmac sha
    private SecretKey getSigningKey() {
        byte[] keyBytes = secretKey.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return Jwts.builder()
                .claims(extraClaims) //Thông tin phụ muốn đưa vào
                .subject(userDetails.getUsername()) //Tên
                .issuedAt(new Date(System.currentTimeMillis())) //Thời điểm tạo
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration)) //Hết hạn khi nào
                .signWith(getSigningKey()) //Thêm secret key
                .compact(); //Dồn lại thành 1 token
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

    //Kiểm tra coi tên có đúng không && token hết hạn chưa
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
}
