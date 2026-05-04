package com.uit.backend_cinema.modules.seat.infrastructure.persistence;

import com.uit.backend_cinema.modules.seat.domain.entity.Seat;
import com.uit.backend_cinema.modules.seat.domain.repository.SeatRepository;
import com.uit.backend_cinema.modules.seat.infrastructure.mapper.SeatInfraMapper;
import com.uit.backend_cinema.modules.seat.infrastructure.repository.JpaSeatRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class SeatRepositoryImpl implements SeatRepository {

    private final JpaSeatRepository jpaSeatRepository;
    private final SeatInfraMapper mapper;

    public SeatRepositoryImpl(JpaSeatRepository jpaSeatRepository, SeatInfraMapper mapper) {
        this.jpaSeatRepository = jpaSeatRepository;
        this.mapper = mapper;
    }

    @Override
    public List<Seat> findByRoomId(Long roomId) {
        return jpaSeatRepository.findByRoomIdAndIsDeletedFalse(roomId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}
