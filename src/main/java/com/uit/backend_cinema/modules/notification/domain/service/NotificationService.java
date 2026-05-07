package com.uit.backend_cinema.modules.notification.domain.service;

import java.util.List;
import java.util.Random;

import org.springframework.stereotype.Service;

import com.uit.backend_cinema.common.exception.BusinessException;
import com.uit.backend_cinema.common.exception.ErrorCode;
import com.uit.backend_cinema.modules.notification.domain.repository.EmailSender;
import com.uit.backend_cinema.modules.notification.domain.repository.OtpStorage;
import com.uit.backend_cinema.modules.ticket.domain.entity.Ticket;

@Service
public class NotificationService {
    private final EmailSender emailSender;
    private final OtpStorage otpStorage;

    private static final long OTP_VALID_DURATION = 5;

    public NotificationService(EmailSender emailSender, OtpStorage otpStorage) {
        this.emailSender = emailSender;
        this.otpStorage = otpStorage;
    }

    public void sendOtp(String email) {
        String otp = String.valueOf(100000 + new Random().nextInt(900000));
        otpStorage.save("OTP: " + email, otp, OTP_VALID_DURATION);
        emailSender.sendEmail(email, "OTP", "Mã xác nhận của bạn: " + otp);
    }

    public boolean verifyOtp(String email, String otp) {
        String key = "OTP: " + email;
        String storedOtp = otpStorage.get(key);
        if (storedOtp != null && storedOtp.equals(otp)) {
            otpStorage.delete(key);
            return true;
        }
        else {
            throw new BusinessException("OTP không hợp lệ hoặc đã hết hạn", ErrorCode.OTP_INVALID);
        }
    }

    public void sendTicketEmail(String email, Long bookingId, List<Ticket> tickets) {
        String subject = "Vé xem phim của bạn - Booking #" + bookingId;
        emailSender.sendEmail(email, subject, buildTicketEmailContent(bookingId, tickets));
    }

    private String buildTicketEmailContent(Long bookingId, List<Ticket> tickets) {
        StringBuilder content = new StringBuilder();
        content.append("Cảm ơn bạn đã thanh toán thành công.\n\n");
        content.append("Booking ID: ").append(bookingId).append("\n");
        content.append("Danh sách vé:\n");

        for (Ticket ticket : tickets) {
            content.append("- Ticket #")
                    .append(ticket.getTicketId())
                    .append(", Seat #")
                    .append(ticket.getSeatId())
                    .append(", QR: TICKET:")
                    .append(ticket.getTicketId())
                    .append(":BOOKING:")
                    .append(bookingId)
                    .append(":SEAT:")
                    .append(ticket.getSeatId())
                    .append("\n");
        }

        return content.toString();
    }
}
