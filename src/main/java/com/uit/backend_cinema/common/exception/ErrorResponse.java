package com.uit.backend_cinema.common.exception;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ErrorResponse {
    private LocalDateTime timestamp;
    private int status;     // Mã lỗi HTTP (400, 404, 500...)
    private String error;
    private String code;     // Mã lỗi cụ thể cho FE (ví dụ: TOKEN_EXPIRED, INVALID_CREDENTIALS...)
    private String message;
    private Object details;  // Các chi tiết thêm (ví dụ: danh sách các field nhập sai)
}
