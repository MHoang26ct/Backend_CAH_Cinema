package com.uit.backend_cinema.modules.seat.domain.repository;

import java.util.List;
import java.util.Optional;

import com.uit.backend_cinema.modules.seat.domain.entity.Seat;

public interface SeatRepository {
    List<Seat> findByRoomId(Long roomId);

    Optional<Seat> findById(Long seatId);

    List<Seat> findByIds(List<Long> seatIds);

    void createSeatMap(List<Seat> seats);

    void softDeleteByRoomId(Long roomId);

    void softDeleteByRoomIds(List<Long> roomIds);

    boolean existsByRoomId(Long roomId);
}
