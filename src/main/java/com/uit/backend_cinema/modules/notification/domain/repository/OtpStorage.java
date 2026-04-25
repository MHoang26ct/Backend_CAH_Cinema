package com.uit.backend_cinema.modules.notification.domain.repository;

public interface OtpStorage {
    void save(String key, String otp, long expirationMinutes);
    String get(String key);
    void delete(String key);
}
