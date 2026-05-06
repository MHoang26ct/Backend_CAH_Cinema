package com.uit.backend_cinema.common.exception;

// Lớp exception để xử lý các lỗi do logic nghiệp vụ.
// Không import bất kỳ thư viện Spring/Web nào — domain thuần túy.
public class BusinessException extends RuntimeException {
    private final ErrorCode code;

    public BusinessException(String message, ErrorCode code) {
        super(message);
        this.code = code;
    }

    public BusinessException(String message, ErrorCode code, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public ErrorCode getCode() {
        return code;
    }
}
