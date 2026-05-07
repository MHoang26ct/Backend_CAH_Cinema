package com.uit.backend_cinema.common.exception;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Helper
    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String errorPhrase, ErrorCode code,
            String message, Object details) {
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
            case EMAIL_ALREADY_EXISTS,
                 BOOKING_EXPIRED,
                 BOOKING_INVALID_STATUS,
                 VOUCHER_HOLD_EXPIRED,
                 PAYMENT_REF_DUPLICATE,
                 PAYMENT_ALREADY_CONFIRMED ->
                    HttpStatus.CONFLICT;

            case INVALID_CREDENTIALS,
                 TOKEN_EXPIRED,
                 ACCESS_TOKEN_EXPIRED,
                 UNAUTHORIZED ->
                    HttpStatus.UNAUTHORIZED;

            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case RESOURCE_NOT_FOUND -> HttpStatus.NOT_FOUND;

            case INVALID_GOOGLE_TOKEN,
                 TOKEN_INVALID,
                 OTP_INVALID,
                 ACCESS_TOKEN_INVALID,
                 DUPLICATE_RESOURCE,
                 SEAT_ALREADY_BOOKED,
                 VALIDATION_FAILED ->
                    HttpStatus.BAD_REQUEST;

            case INTERNAL_ERROR,
                 OUTBOX_EVENT_CREATE_FAILED,
                 OUTBOX_PAYLOAD_SERIALIZATION_FAILED,
                 TICKET_CREATE_FAILED -> HttpStatus.INTERNAL_SERVER_ERROR;
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

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleHandlerMethodValidation(HandlerMethodValidationException ex) {
        Map<String, String> validationErrors = ex.getAllValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> Map.entry(
                                resolveValidationField(result.getMethodParameter().getParameterName(),
                                        result.getContainerIndex(), error),
                                error.getDefaultMessage() != null ? error.getDefaultMessage() : "Giá trị không hợp lệ")))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (first, second) -> first
                ));
        return buildResponse(HttpStatus.BAD_REQUEST, "Validation Failed", ErrorCode.VALIDATION_FAILED,
                "Dữ liệu đầu vào không hợp lệ", validationErrors);
    }

    private String resolveValidationField(String parameterName, Integer containerIndex, MessageSourceResolvable error) {
        StringBuilder field = new StringBuilder(parameterName != null ? parameterName : "request");
        if (containerIndex != null) {
            field.append('[').append(containerIndex).append(']');
        }
        if (error instanceof FieldError fieldError) {
            field.append('.').append(fieldError.getField());
        }
        return field.toString();
    }

    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<ErrorResponse> handlePropertyReferenceException(PropertyReferenceException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Tham số đầu vào không hợp lệ", ErrorCode.VALIDATION_FAILED, ex.getMessage(), null);
    }

    // IllegalArgumentException — thường do logic đơn giản trong service
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Bad Request", ErrorCode.VALIDATION_FAILED, ex.getMessage(), null);
    }

    // Spring Security Authentication — User không tồn tại khi load từ DB
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUsernameNotFound(UsernameNotFoundException ex) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "Unauthorized", ErrorCode.INVALID_CREDENTIALS, ex.getMessage(),
                null);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Bad Request", ErrorCode.VALIDATION_FAILED,
                "Dữ liệu đầu vào không đúng định dạng hoặc sai kiểu dữ liệu", null);
    }

    // CATCH-ALL 500
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        log.error("[UNHANDLED EXCEPTION] {}: {}", ex.getClass().getSimpleName(), ex.getMessage(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                ErrorCode.INTERNAL_ERROR, "Oops! Hệ thống đang gặp vấn đề, vui lòng thử lại sau nhé <3!", null);
    }
}
