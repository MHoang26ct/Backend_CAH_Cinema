package com.uit.backend_cinema.modules.seat.domain.repository;

import java.util.List;

import com.uit.backend_cinema.modules.seat.domain.entity.SeatType;

public interface SeatTypeRepository {
    List<SeatType> getAllSeatTypes();

    List<SeatType> getSeatTypesByIds(List<Long> seatTypeIds);
}
