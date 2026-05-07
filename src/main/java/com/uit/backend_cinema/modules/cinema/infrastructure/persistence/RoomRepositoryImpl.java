package com.uit.backend_cinema.modules.cinema.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.uit.backend_cinema.modules.cinema.domain.entity.Room;
import com.uit.backend_cinema.modules.cinema.domain.repository.RoomRepository;
import com.uit.backend_cinema.modules.cinema.infrastructure.mapper.RoomInfraMapper;
import com.uit.backend_cinema.modules.cinema.infrastructure.repository.JpaRoomRepository;

@Repository
public class RoomRepositoryImpl implements RoomRepository {

    private final JpaRoomRepository jpaRepository;
    private final RoomInfraMapper mapper;

    public RoomRepositoryImpl(JpaRoomRepository jpaRepository, RoomInfraMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<Room> findById(long roomId) {
        return jpaRepository.findById(roomId).map(mapper::toDomain);
    }

    @Override
    public List<Room> findAllByCinemaId(long cinemaId) {
        return jpaRepository.findAllByCinemaId(cinemaId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Room save(Room room) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(room)));
    }

    @Override
    public void delete(Room room) {
        jpaRepository.save(mapper.toEntity(room));
    }

    @Override
    public void softDeleteByCinemaId(long cinemaId) {
        jpaRepository.softDeleteByCinemaId(cinemaId);
    }
}
