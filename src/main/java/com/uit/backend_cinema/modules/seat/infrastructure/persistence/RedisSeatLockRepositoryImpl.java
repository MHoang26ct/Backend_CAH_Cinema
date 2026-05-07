package com.uit.backend_cinema.modules.seat.infrastructure.persistence;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import com.uit.backend_cinema.modules.seat.domain.repository.SeatLockRepository;

@Repository
public class RedisSeatLockRepositoryImpl implements SeatLockRepository{
     // Key format: lock:showtime:{showtimeId}:seat:{seatId}
    private static final String KEY_FORMAT = "lock:showtime:%d:seat:%d";

    private final StringRedisTemplate redisTemplate;

    public RedisSeatLockRepositoryImpl(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private String buildKey(Long showtimeId, Long seatId) {
        return String.format(KEY_FORMAT, showtimeId, seatId);
    }

    @Override
    public boolean lockSeat(Long showtimeId, Long seatId, Long userId, long ttlSeconds) {
        String key = buildKey(showtimeId, seatId);
        // setIfAbsent = chỉ set nếu key chưa tồn tại (atomic operation)
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(key, String.valueOf(userId), ttlSeconds, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(success);
    }

    @Override
    public boolean promoteLockIfOwner(Long showtimeId, Long seatId, Long userId, long ttlSeconds) {
        String key = buildKey(showtimeId, seatId);
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText("""
                local value = redis.call('GET', KEYS[1])
                if value == ARGV[1] then
                    return redis.call('EXPIRE', KEYS[1], ARGV[2])
                end
                return 0
                """);
        script.setResultType(Long.class);
        Long result = redisTemplate.execute(
                script,
                Collections.singletonList(key),
                String.valueOf(userId),
                String.valueOf(ttlSeconds)
        );
        return result != null && result == 1L;
    }

    @Override
    public boolean isLocked(Long showtimeId, Long seatId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(buildKey(showtimeId, seatId)));
    }

    @Override
    public void unlock(Long showtimeId, Long seatId) {
        redisTemplate.delete(buildKey(showtimeId, seatId));
    }

    @Override
    public String getLockedBy(Long showtimeId, Long seatId) {
        return redisTemplate.opsForValue().get(buildKey(showtimeId, seatId));
    }
}
