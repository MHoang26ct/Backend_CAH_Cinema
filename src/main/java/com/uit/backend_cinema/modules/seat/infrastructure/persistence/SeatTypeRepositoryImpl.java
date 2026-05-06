package com.uit.backend_cinema.modules.seat.infrastructure.persistence;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.uit.backend_cinema.modules.seat.domain.entity.SeatType;
import com.uit.backend_cinema.modules.seat.domain.repository.SeatTypeRepository;
import com.uit.backend_cinema.modules.seat.infrastructure.mapper.SeatInfraMapper;
import com.uit.backend_cinema.modules.seat.infrastructure.repository.JpaSeatTypeRepository;

@Repository
public class SeatTypeRepositoryImpl implements SeatTypeRepository {

    private final JpaSeatTypeRepository jpaSeatTypeRepository;
    private final SeatInfraMapper mapper;

    public SeatTypeRepositoryImpl(JpaSeatTypeRepository jpaSeatTypeRepository, SeatInfraMapper mapper) {
        this.jpaSeatTypeRepository = jpaSeatTypeRepository;
        this.mapper = mapper;
    }

    @Override
    public List<SeatType> getAllSeatTypes() {
        return jpaSeatTypeRepository.findAll()
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<SeatType> getSeatTypesByIds(List<Long> seatTypeIds) {
        return jpaSeatTypeRepository.findAllById(seatTypeIds)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}
