package com.uit.backend_cinema.modules.notification.domain.repository;

public interface OtpStorage {
    void save(String key, String otp, long expirationMinutes);
    // Registration OTPs: 5-minute validity, 60-second resend cooldown, 5 attempts.
    boolean saveRegistrationOtp(String email, String otp);
    boolean consumeRegistrationOtp(String email, String otp);
    String get(String key);
    void delete(String key);
}
