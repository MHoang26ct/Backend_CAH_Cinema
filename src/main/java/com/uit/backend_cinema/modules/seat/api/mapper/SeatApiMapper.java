package com.uit.backend_cinema.modules.seat.api.mapper;

import com.uit.backend_cinema.modules.seat.api.dto.CreateSeatDTO;
import com.uit.backend_cinema.modules.seat.api.dto.SeatDTO;
import com.uit.backend_cinema.modules.seat.api.dto.SeatTypeDTO;
import com.uit.backend_cinema.modules.seat.api.util.SeatLabelUtil;
import com.uit.backend_cinema.modules.seat.domain.entity.Seat;
import com.uit.backend_cinema.modules.seat.domain.entity.SeatType;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", imports = SeatLabelUtil.class)
public interface SeatApiMapper {

    @Mapping(target = "row", source = "seatRow")
    @Mapping(target = "col", source = "seatCol")
    @Mapping(target = "rowLabel", expression = "java(SeatLabelUtil.toRowLabel(seat.getSeatRow()))")
    @Mapping(target = "colLabel", expression = "java(SeatLabelUtil.toColLabel(seat.getSeatCol()))")
    SeatDTO toDto(Seat seat);

    SeatTypeDTO toDto(SeatType seatType);

    @Mapping(target = "seatType.seatTypeId", source = "seatTypeId")
    @Mapping(target = "seatRow", source = "row")
    @Mapping(target = "seatCol", source = "col")
    @Mapping(target = "isDeleted", constant = "false")
    @Mapping(target = "seatId", ignore = true)
    @Mapping(target = "roomId", ignore = true)
    @Mapping(target = "isLocked", constant = "false")
    @Mapping(target = "status", constant = "ACTIVE")
    Seat toDomain(CreateSeatDTO createSeatDTO);
}
