package com.uit.backend_cinema.modules.cinema.domain.service;

import com.uit.backend_cinema.common.exception.BusinessException;
import com.uit.backend_cinema.common.exception.ErrorCode;
import com.uit.backend_cinema.modules.cinema.domain.entity.Cinema;
import com.uit.backend_cinema.modules.cinema.domain.entity.Room;
import com.uit.backend_cinema.modules.cinema.domain.repository.CinemaRepository;
import com.uit.backend_cinema.modules.cinema.domain.repository.RoomRepository;
import com.uit.backend_cinema.modules.seat.domain.repository.SeatRepository;
import com.uit.backend_cinema.modules.showtime.domain.repository.ShowtimeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CinemaService {

    private final CinemaRepository cinemaRepository;
    private final RoomRepository roomRepository;
    private final SeatRepository seatRepository;
    private final ShowtimeRepository showtimeRepository;

    public CinemaService(CinemaRepository cinemaRepository, RoomRepository roomRepository,
            SeatRepository seatRepository, ShowtimeRepository showtimeRepository) {
        this.cinemaRepository = cinemaRepository;
        this.roomRepository = roomRepository;
        this.seatRepository = seatRepository;
        this.showtimeRepository = showtimeRepository;
    }

    public Cinema findById(long cinemaId) {
        return cinemaRepository.findById(cinemaId)
                .orElseThrow(() -> new BusinessException("Rạp chiếu phim không tồn tại", ErrorCode.RESOURCE_NOT_FOUND));
    }

    public List<Cinema> findAll() {
        return cinemaRepository.findAll();
    }

    @Transactional
    public Cinema create(Cinema cinema) {
        return cinemaRepository.save(cinema);
    }

    @Transactional
    public Cinema update(Cinema updatedCinema) {
        Cinema existing = cinemaRepository.findById(updatedCinema.getCinemaId())
                .orElseThrow(() -> new BusinessException("Rạp chiếu phim không tồn tại", ErrorCode.RESOURCE_NOT_FOUND));
        existing.setName(updatedCinema.getName());
        existing.setAddress(updatedCinema.getAddress());
        existing.setHotline(updatedCinema.getHotline());
        return cinemaRepository.save(existing);
    }

    @Transactional
    public void delete(long cinemaId) {
        Cinema existing = cinemaRepository.findById(cinemaId)
                .orElseThrow(() -> new BusinessException("Rạp chiếu phim không tồn tại", ErrorCode.RESOURCE_NOT_FOUND));
        List<Long> roomIds = roomRepository.findAllByCinemaId(cinemaId).stream()
                .map(Room::getRoomId)
                .toList();
        showtimeRepository.softDeleteByRoomIds(roomIds);
        seatRepository.softDeleteByRoomIds(roomIds);
        roomRepository.softDeleteByCinemaId(cinemaId);
        existing.setDeleted(true);
        cinemaRepository.save(existing);
    }
}
