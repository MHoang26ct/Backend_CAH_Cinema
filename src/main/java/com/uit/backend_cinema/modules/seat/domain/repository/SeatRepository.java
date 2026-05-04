package com.uit.backend_cinema.modules.seat.domain.repository;

import com.uit.backend_cinema.modules.seat.domain.entity.Seat;
import java.util.List;

public interface SeatRepository {
     List<Seat> findByRoomId(Long roomId);
}
