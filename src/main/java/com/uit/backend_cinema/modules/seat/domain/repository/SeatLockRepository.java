package com.uit.backend_cinema.modules.seat.domain.repository;

public interface  SeatLockRepository {
    boolean lockSeat(Long showtimeId, Long seatId, Long userId, long ttlSeconds);

    boolean isLocked(Long showtimeId, Long seatId);

    void unlock(Long showtimeId, Long seatId);

    String getLockedBy(Long showtimeId, Long seatId);
}
