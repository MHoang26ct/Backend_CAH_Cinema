package com.uit.backend_cinema.modules.notification.domain.service;

import com.uit.backend_cinema.common.exception.BusinessException;
import com.uit.backend_cinema.common.exception.ErrorCode;
import com.uit.backend_cinema.modules.notification.domain.repository.EmailSender;
import com.uit.backend_cinema.modules.notification.domain.repository.OtpStorage;
import org.springframework.stereotype.Service;

import java.util.Random;

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
}
