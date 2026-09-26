package com.uit.backend_cinema.modules.notification.infrastructure.persistence;

import java.time.Duration;
import java.util.List;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
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

    private static final DefaultRedisScript<Long> SAVE_REGISTRATION = new DefaultRedisScript<>("""
            if redis.call('EXISTS', KEYS[2]) == 1 then return 0 end
            redis.call('SET', KEYS[1], ARGV[1], 'EX', 300)
            redis.call('SET', KEYS[2], '1', 'EX', 60)
            redis.call('DEL', KEYS[3])
            return 1
            """, Long.class);

    private static final DefaultRedisScript<Long> CONSUME_REGISTRATION = new DefaultRedisScript<>("""
            local expected = redis.call('GET', KEYS[1])
            if not expected then return 0 end
            if expected == ARGV[1] then
                redis.call('DEL', KEYS[1], KEYS[3])
                return 1
            end
            local attempts = redis.call('INCR', KEYS[3])
            if attempts == 1 then redis.call('EXPIRE', KEYS[3], 300) end
            if attempts >= 5 then redis.call('DEL', KEYS[1], KEYS[3]) end
            return 0
            """, Long.class);

    private List<Object> registrationKeys(String email) {
        String key = "OTP:REGISTER:" + email;
        return List.of(key, key + ":cooldown", key + ":attempts");
    }

    @Override
    public boolean saveRegistrationOtp(String email, String otp) {
        return Long.valueOf(1).equals(redisTemplate.execute(
                SAVE_REGISTRATION, registrationKeys(email), otp));
    }

    @Override
    public boolean consumeRegistrationOtp(String email, String otp) {
        return Long.valueOf(1).equals(redisTemplate.execute(
                CONSUME_REGISTRATION, registrationKeys(email), otp));
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
