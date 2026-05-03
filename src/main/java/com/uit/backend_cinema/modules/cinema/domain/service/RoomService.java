package com.uit.backend_cinema.modules.cinema.domain.service;

import com.uit.backend_cinema.common.exception.BusinessException;
import com.uit.backend_cinema.common.exception.ErrorCode;
import com.uit.backend_cinema.modules.cinema.domain.entity.Room;
import com.uit.backend_cinema.modules.cinema.domain.repository.CinemaRepository;
import com.uit.backend_cinema.modules.cinema.domain.repository.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class RoomService {

    private final RoomRepository roomRepository;
    private final CinemaRepository cinemaRepository;

    public RoomService(RoomRepository roomRepository, CinemaRepository cinemaRepository) {
        this.roomRepository = roomRepository;
        this.cinemaRepository = cinemaRepository;
    }

    public Room findById(long roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException("Phòng chiếu không tồn tại", ErrorCode.RESOURCE_NOT_FOUND));
    }

    public List<Room> findAllByCinemaId(long cinemaId) {
        // Kiểm tra cinema tồn tại
        cinemaRepository.findById(cinemaId)
                .orElseThrow(() -> new BusinessException("Rạp chiếu phim không tồn tại", ErrorCode.RESOURCE_NOT_FOUND));
        return roomRepository.findAllByCinemaId(cinemaId);
    }

    @Transactional
    public Room create(Room room) {
        // Kiểm tra cinema tồn tại trước khi tạo phòng
        cinemaRepository.findById(room.getCinemaId())
                .orElseThrow(() -> new BusinessException("Rạp chiếu phim không tồn tại", ErrorCode.RESOURCE_NOT_FOUND));
        return roomRepository.save(room);
    }

    @Transactional
    public Room update(Room updatedRoom) {
        Room existing = roomRepository.findById(updatedRoom.getRoomId())
                .orElseThrow(() -> new BusinessException("Phòng chiếu không tồn tại", ErrorCode.RESOURCE_NOT_FOUND));
        existing.setRoomName(updatedRoom.getRoomName());
        return roomRepository.save(existing);
    }

    @Transactional
    public void delete(long roomId) {
        Room existing = roomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException("Phòng chiếu không tồn tại", ErrorCode.RESOURCE_NOT_FOUND));
        existing.setDeleted(true);
        roomRepository.save(existing);
    }
}
