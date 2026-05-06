package com.uit.backend_cinema.modules.seat.domain.repository;

import com.uit.backend_cinema.modules.seat.domain.entity.Seat;

import java.util.List;
import java.util.Optional;

public interface SeatRepository {
    List<Seat> findByRoomId(Long roomId);

    Optional<Seat> findById(Long seatId);

    List<Seat> findByIds(List<Long> seatIds);

    void createSeatMap(List<Seat> seats);

    void softDeleteByRoomId(Long roomId);

    boolean existsByRoomId(Long roomId);
}
