package com.uit.backend_cinema.modules.ticket.domain.service;

import com.uit.backend_cinema.common.exception.BusinessException;
import com.uit.backend_cinema.common.exception.ErrorCode;
import com.uit.backend_cinema.modules.ticket.domain.entity.Ticket;
import com.uit.backend_cinema.modules.ticket.domain.repository.TicketRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TicketService {
    private final TicketRepository ticketRepository;

    public TicketService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    @Transactional
    public void createTicketIfAbsent(List<Ticket> tickets) {
        if (!validateTickets(tickets)) {
            return;
        }
        try {
            ticketRepository.saveAll(tickets);
        } catch (DataIntegrityViolationException ex) {
            if (hasSameExistingTickets(tickets)) {
                return;
            }
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public List<Ticket> findAllByBookingId(Long bookingId) {
        return ticketRepository.findAllByBookingId(bookingId);
    }

    private boolean validateTickets(List<Ticket> tickets) {
        if (tickets == null || tickets.isEmpty()) {
            return false;
        }
        tickets.forEach(ticket -> {
            if (ticket.getBookingId() == null || ticket.getSeatId() == null) {
                throw new BusinessException("BookingId và SeatId không được null", ErrorCode.VALIDATION_FAILED);
            }
        });
        Long bookingId = tickets.get(0).getBookingId();
        if (!tickets.stream().allMatch(ticket -> ticket.getBookingId().equals(bookingId))) {
            throw new BusinessException("Tất cả các vé phải có cùng một bookingId", ErrorCode.VALIDATION_FAILED);
        }
        if (tickets.stream().map(Ticket::getSeatId).distinct().count() != tickets.size()) {
            throw new BusinessException("Danh sách vé có seatId trùng lặp", ErrorCode.VALIDATION_FAILED);
        }
        if (hasSameExistingTickets(tickets)) {
            return false;
        }
        if (!ticketRepository.findAllByBookingId(bookingId).isEmpty()) {
            throw new BusinessException("Booking đã có vé, không thể tạo thêm", ErrorCode.VALIDATION_FAILED);
        }
        return true;
    }

    private boolean hasSameExistingTickets(List<Ticket> tickets) {
        if (tickets == null || tickets.isEmpty()) {
            return false;
        }
        Long bookingId = tickets.get(0).getBookingId();
        List<Ticket> existingTickets = ticketRepository.findAllByBookingId(bookingId);
        if (existingTickets.size() != tickets.size()) {
            return false;
        }
        return existingTickets.stream().map(Ticket::getSeatId).collect(java.util.stream.Collectors.toSet())
                .equals(tickets.stream().map(Ticket::getSeatId).collect(java.util.stream.Collectors.toSet()));
    }
}
