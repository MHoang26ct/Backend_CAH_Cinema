package com.uit.backend_cinema.modules.notification.domain.repository;

public interface EmailSender {
    void sendEmail(String to, String subject, String content);
}
