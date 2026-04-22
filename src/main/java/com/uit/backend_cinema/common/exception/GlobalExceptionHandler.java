package com.uit.backend_cinema.common.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Helper
    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String errorPhrase, ErrorCode code, String message, Object details) {
        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(errorPhrase)
                .code(code != null ? code.name() : null)
                .message(message)
                .details(details)
                .build();
        return ResponseEntity.status(status).body(body);
    }

    // Map ErrorCode → HttpStatus — chỉ tầng handler mới biết HTTP status
    private HttpStatus resolveStatus(ErrorCode code) {
        return switch (code) {
            case EMAIL_ALREADY_EXISTS                   -> HttpStatus.CONFLICT;
            case INVALID_CREDENTIALS,
                 TOKEN_EXPIRED,
                 ACCESS_TOKEN_EXPIRED,
                 UNAUTHORIZED                           -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN                              -> HttpStatus.FORBIDDEN;
            case RESOURCE_NOT_FOUND                    -> HttpStatus.NOT_FOUND;
            case INVALID_GOOGLE_TOKEN,
                 TOKEN_INVALID,
                 ACCESS_TOKEN_INVALID,
                 VALIDATION_FAILED                     -> HttpStatus.BAD_REQUEST;
            case INTERNAL_ERROR                        -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    // Lỗi nghiệp vụ có mã lỗi cụ thể — ném từ Service
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex) {
        HttpStatus status = resolveStatus(ex.getCode());
        return buildResponse(status, status.getReasonPhrase(), ex.getCode(), ex.getMessage(), null);
    }

    // 400 BAD REQUEST
    // Lỗi validate @Valid — trả về chi tiết từng field
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Giá trị không hợp lệ",
                        (first, second) -> first // giữ lỗi đầu tiên nếu một field có nhiều lỗi
                ));
        return buildResponse(HttpStatus.BAD_REQUEST, "Validation Failed", ErrorCode.VALIDATION_FAILED,
                "Dữ liệu đầu vào không hợp lệ", fieldErrors);
    }

    // IllegalArgumentException — thường do logic đơn giản trong service
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Bad Request", ErrorCode.VALIDATION_FAILED, ex.getMessage(), null);
    }

    // Spring Security Authentication — User không tồn tại khi load từ DB
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUsernameNotFound(UsernameNotFoundException ex) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "Unauthorized", ErrorCode.INVALID_CREDENTIALS, ex.getMessage(), null);
    }


    // CATCH-ALL 500
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        log.error("[UNHANDLED EXCEPTION] {}: {}", ex.getClass().getSimpleName(), ex.getMessage(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                ErrorCode.INTERNAL_ERROR, "Oops! Hệ thống đang gặp vấn đề, vui lòng thử lại sau nhé <3!", null);
    }
}
