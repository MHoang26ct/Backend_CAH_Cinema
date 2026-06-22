package com.uit.backend_cinema.unit_test;

import com.uit.backend_cinema.modules.notification.domain.repository.EmailSender;
import com.uit.backend_cinema.modules.notification.domain.repository.OtpStorage;
import com.uit.backend_cinema.modules.notification.domain.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationModuleServiceTest {

    @Test
    @DisplayName("Notification module: verify OTP đúng sẽ xóa OTP đã dùng")
    void verifyOtpDeletesMatchedOtp() {
        EmailSender emailSender = mock(EmailSender.class);
        OtpStorage otpStorage = mock(OtpStorage.class);
        com.uit.backend_cinema.common.util.JwtUtil jwtUtil = mock(com.uit.backend_cinema.common.util.JwtUtil.class);
        NotificationService notificationService = new NotificationService(emailSender, otpStorage, jwtUtil);

        when(otpStorage.get("OTP: user@cah.vn")).thenReturn("123456");

        assertTrue(notificationService.verifyOtp("user@cah.vn", "123456"));
        verify(otpStorage).delete("OTP: user@cah.vn");
    }
}
