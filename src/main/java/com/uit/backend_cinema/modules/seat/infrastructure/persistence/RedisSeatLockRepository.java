package com.uit.backend_cinema.modules.seat.infrastructure.persistence;

import com.uit.backend_cinema.modules.seat.domain.repository.SeatLockRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.concurrent.TimeUnit;

@Repository
public class RedisSeatLockRepository implements SeatLockRepository{
     // Key format: lock:showtime:{showtimeId}:seat:{seatId}
    private static final String KEY_FORMAT = "lock:showtime:%d:seat:%d";

    private final StringRedisTemplate redisTemplate;

    public RedisSeatLockRepository(StringRedisTemplate redisTemplate) {
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
