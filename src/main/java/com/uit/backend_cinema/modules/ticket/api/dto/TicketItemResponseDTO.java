package com.uit.backend_cinema.modules.ticket.api.dto;

import java.math.BigDecimal;

import com.uit.backend_cinema.modules.ticket.domain.entity.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketItemResponseDTO {
    private Long ticketId;
    private Long bookingId;
    private Long showtimeId;
    private Long seatId;
    private String seatName;
    private BigDecimal price;
    private String checkinCode;
    /**
     * Chuỗi dùng để vẽ mã QR trên UI (bằng checkinCode).
     * Frontend có thể ném chuỗi này vào bất kỳ thư viện tạo QR nào (ví dụ qrcode.react).
     */
    private String qrCode;
    private TicketStatus status;
    private Boolean isCheckedIn;
}
