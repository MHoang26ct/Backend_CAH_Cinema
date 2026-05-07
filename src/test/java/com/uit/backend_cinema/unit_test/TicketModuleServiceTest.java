package com.uit.backend_cinema.unit_test;

import com.uit.backend_cinema.common.exception.BusinessException;
import com.uit.backend_cinema.modules.ticket.domain.repository.PendingTicketItemRepository;
import com.uit.backend_cinema.modules.ticket.domain.repository.TicketRepository;
import com.uit.backend_cinema.modules.ticket.domain.service.TicketService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TicketModuleServiceTest {

    @Test
    @DisplayName("Ticket module: validate sold seat ném lỗi nếu ghế đã bán")
    void validateSeatsNotSoldRejectsSoldSeat() {
        TicketRepository ticketRepository = mock(TicketRepository.class);
        PendingTicketItemRepository pendingTicketItemRepository = mock(PendingTicketItemRepository.class);
        TicketService ticketService = new TicketService(ticketRepository, pendingTicketItemRepository);

        when(ticketRepository.existsSoldSeatByShowtimeIdAndSeatIds(3L, List.of(4L))).thenReturn(true);

        assertThrows(BusinessException.class, () -> ticketService.validateSeatsNotSold(3L, List.of(4L)));
    }
}
