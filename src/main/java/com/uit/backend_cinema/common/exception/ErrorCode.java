package com.uit.backend_cinema.common.exception;

/**
 * Enum chứa các mã lỗi cụ thể để FE xử lý từng trường hợp.
 * Quy tắc đặt tên: DOMAIN_PROBLEM (SCREAMING_SNAKE_CASE)
 */
public enum ErrorCode {

    // Auth
    EMAIL_ALREADY_EXISTS,       // Email đã được đăng ký
    INVALID_CREDENTIALS,        // Sai email hoặc mật khẩu
    INVALID_GOOGLE_TOKEN,       // Google ID Token không hợp lệ

    // Token
    TOKEN_EXPIRED,              // Refresh token đã hết hạn
    TOKEN_INVALID,              // Refresh token không tồn tại / sai
    OTP_INVALID,                // OTP không hợp lệ

    ACCESS_TOKEN_EXPIRED,       // Access token (JWT) đã hết hạn (từ filter)
    ACCESS_TOKEN_INVALID,       // Access token bị giả mạo / không hợp lệ

    // Authorization
    UNAUTHORIZED,               // Chưa đăng nhập
    FORBIDDEN,                  // Không có quyền

    // Resource
    RESOURCE_NOT_FOUND,         // Tài nguyên không tồn tại
    DUPLICATE_RESOURCE,         // Tài nguyên đã tồn tại

    // Validation
    VALIDATION_FAILED,          // Dữ liệu đầu vào không hợp lệ (@Valid)

    // Server
    INTERNAL_ERROR,             // Lỗi server không xác định
}
