package com.uit.backend_cinema.modules.seat.api.mapper;

import com.uit.backend_cinema.modules.seat.api.dto.SeatDTO;
import com.uit.backend_cinema.modules.seat.api.dto.SeatTypeDTO;
import com.uit.backend_cinema.modules.seat.domain.entity.Seat;
import com.uit.backend_cinema.modules.seat.domain.entity.SeatType;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SeatApiMapper {
    SeatDTO toDto(Seat seat);
    SeatTypeDTO toDto(SeatType seatType);
}
