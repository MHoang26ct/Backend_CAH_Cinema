package com.uit.backend_cinema.unit_test;

import com.uit.backend_cinema.modules.cinema.domain.entity.Cinema;
import com.uit.backend_cinema.modules.cinema.domain.entity.Room;
import com.uit.backend_cinema.modules.cinema.domain.repository.CinemaRepository;
import com.uit.backend_cinema.modules.cinema.domain.repository.RoomRepository;
import com.uit.backend_cinema.modules.cinema.domain.service.CinemaService;
import com.uit.backend_cinema.modules.seat.domain.repository.SeatRepository;
import com.uit.backend_cinema.modules.showtime.domain.repository.ShowtimeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CinemaModuleServiceTest {

    @Test
    @DisplayName("Cinema module: xóa rạp sẽ soft delete room, seat và showtime liên quan")
    void deleteSoftDeletesDependentResources() {
        CinemaRepository cinemaRepository = mock(CinemaRepository.class);
        RoomRepository roomRepository = mock(RoomRepository.class);
        SeatRepository seatRepository = mock(SeatRepository.class);
        ShowtimeRepository showtimeRepository = mock(ShowtimeRepository.class);
        CinemaService cinemaService = new CinemaService(cinemaRepository, roomRepository, seatRepository, showtimeRepository);
        Cinema cinema = new Cinema();
        cinema.setCinemaId(1L);
        Room room = new Room();
        room.setRoomId(2L);

        when(cinemaRepository.findById(1L)).thenReturn(Optional.of(cinema));
        when(roomRepository.findAllByCinemaId(1L)).thenReturn(List.of(room));

        cinemaService.delete(1L);

        verify(showtimeRepository).softDeleteByRoomIds(List.of(2L));
        verify(seatRepository).softDeleteByRoomIds(List.of(2L));
        verify(roomRepository).softDeleteByCinemaId(1L);
        assertTrue(cinema.isDeleted());
        verify(cinemaRepository).save(cinema);
    }
}
