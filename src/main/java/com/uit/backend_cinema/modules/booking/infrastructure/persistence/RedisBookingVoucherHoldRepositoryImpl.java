package com.uit.backend_cinema.modules.booking.infrastructure.persistence;

import com.uit.backend_cinema.modules.booking.domain.entity.BookingVoucherHold;
import com.uit.backend_cinema.modules.booking.domain.entity.BookingVoucherHoldStatus;
import com.uit.backend_cinema.modules.booking.domain.repository.BookingVoucherHoldRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Repository
public class RedisBookingVoucherHoldRepositoryImpl implements BookingVoucherHoldRepository {
    private static final String BOOKING_HOLD_KEY_PATTERN = "booking:voucher-hold:booking:%d";
    private static final String BOOKING_HOLD_SCAN_PATTERN = "booking:voucher-hold:booking:*";

    private final StringRedisTemplate redisTemplate;

    public RedisBookingVoucherHoldRepositoryImpl(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public BookingVoucherHold save(BookingVoucherHold hold) {
        String key = bookingKey(hold.getBookingId());
        long ttlSeconds = Math.max(1L, Duration.between(LocalDateTime.now(), hold.getExpiresAt()).getSeconds());
        String raw = formatRawValue(hold);
        redisTemplate.opsForValue().set(key, raw, ttlSeconds, TimeUnit.SECONDS);
        return hold;
    }

    @Override
    public long countByVoucherIdAndStatusAndExpiresAtAfter(Long voucherId, BookingVoucherHoldStatus status, LocalDateTime now) {
        if (status != BookingVoucherHoldStatus.HELD) {
            return 0L;
        }
        Set<String> keys = redisTemplate.keys(BOOKING_HOLD_SCAN_PATTERN);
        if (keys == null || keys.isEmpty()) {
            return 0L;
        }
        long count = 0L;
        for (String key : keys) {
            String raw = redisTemplate.opsForValue().get(key);
            if (raw == null) {
                continue;
            }
            BookingVoucherHold hold = parseRawValue(raw);
            if (hold == null) {
                continue;
            }
            if (!voucherId.equals(hold.getVoucherId())) {
                continue;
            }
            if (hold.getStatus() != BookingVoucherHoldStatus.HELD) {
                continue;
            }
            if (!hold.getExpiresAt().isAfter(now)) {
                continue;
            }
            count++;
        }
        return count;
    }

    @Override
    public Optional<BookingVoucherHold> findByBookingId(Long bookingId) {
        String raw = redisTemplate.opsForValue().get(bookingKey(bookingId));
        if (raw == null) {
            return Optional.empty();
        }
        BookingVoucherHold hold = parseRawValue(raw);
        if (hold == null) {
            return Optional.empty();
        }
        return Optional.of(hold);
    }

    @Override
    public void softDeleteByBookingId(Long bookingId) {
        redisTemplate.delete(bookingKey(bookingId));
    }

    private String bookingKey(Long bookingId) {
        return String.format(BOOKING_HOLD_KEY_PATTERN, bookingId);
    }

    private String formatRawValue(BookingVoucherHold hold) {
        long expireEpoch = hold.getExpiresAt().toEpochSecond(ZoneOffset.UTC);
        return hold.getBookingId() + "|" +
                hold.getVoucherId() + "|" +
                hold.getDiscountAmount() + "|" +
                hold.getStatus().name() + "|" +
                expireEpoch;
    }

    private BookingVoucherHold parseRawValue(String raw) {
        String[] parts = raw.split("\\|");
        if (parts.length != 5) {
            return null;
        }
        try {
            BookingVoucherHold hold = new BookingVoucherHold();
            hold.setBookingId(Long.parseLong(parts[0]));
            hold.setVoucherId(Long.parseLong(parts[1]));
            hold.setDiscountAmount(new BigDecimal(parts[2]));
            hold.setStatus(BookingVoucherHoldStatus.valueOf(parts[3]));
            hold.setExpiresAt(LocalDateTime.ofEpochSecond(Long.parseLong(parts[4]), 0, ZoneOffset.UTC));
            return hold;
        } catch (RuntimeException ex) {
            return null;
        }
    }
}
