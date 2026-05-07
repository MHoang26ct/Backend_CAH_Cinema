package com.uit.backend_cinema.modules.notification.infrastructure.persistence;

import java.time.Duration;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import com.uit.backend_cinema.modules.notification.domain.repository.OtpStorage;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class RedisOtpStorageImpl implements OtpStorage {
    private final RedisTemplate<Object, Object> redisTemplate;

    @Override
    public void save(String key, String otp, long expirationMinutes) {
        redisTemplate.opsForValue().set(key, otp, Duration.ofMinutes(expirationMinutes));
    }

    @Override
    public String get(String key) {
        return (String) redisTemplate.opsForValue().get(key);
    }

    @Override
    public void delete(String key) {
        redisTemplate.delete(key);
    }
}
