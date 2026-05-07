package com.uit.backend_cinema.modules.cinema.domain.repository;

import java.util.List;
import java.util.Optional;

import com.uit.backend_cinema.modules.cinema.domain.entity.Room;

public interface RoomRepository {
    Optional<Room> findById(long roomId);
    List<Room> findAllByCinemaId(long cinemaId);
    Room save(Room room);
    void delete(Room room);
    void softDeleteByCinemaId(long cinemaId);
}
