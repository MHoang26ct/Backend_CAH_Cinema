package com.uit.backend_cinema.modules.seat.infrastructure.persistence;

import com.uit.backend_cinema.modules.seat.domain.entity.Seat;
import com.uit.backend_cinema.modules.seat.domain.repository.SeatRepository;
import com.uit.backend_cinema.modules.seat.infrastructure.mapper.SeatInfraMapper;
import com.uit.backend_cinema.modules.seat.infrastructure.repository.JpaSeatRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

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
        return jpaSeatRepository.findByRoomIdOrderBySeatRowAscSeatColAsc(roomId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Seat> findById(Long seatId) {
        return jpaSeatRepository.findById(seatId)
                .map(mapper::toDomain);
    }

    @Override
    public List<Seat> findByIds(List<Long> seatIds) {
        return jpaSeatRepository.findBySeatIdInOrderBySeatRowAscSeatColAsc(seatIds)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public void createSeatMap(List<Seat> seats) {
        jpaSeatRepository.saveAll(seats.stream()
                .map(mapper::toEntity)
                .toList());
    }

    @Override
    public void softDeleteByRoomId(Long roomId) {
        jpaSeatRepository.softDeleteByRoomId(roomId);
    }

    @Override
    public void softDeleteByRoomIds(List<Long> roomIds) {
        if (roomIds == null || roomIds.isEmpty()) {
            return;
        }
        jpaSeatRepository.softDeleteByRoomIds(roomIds);
    }

    @Override
    public boolean existsByRoomId(Long roomId) {
        return jpaSeatRepository.existsByRoomId(roomId);
    }
}
