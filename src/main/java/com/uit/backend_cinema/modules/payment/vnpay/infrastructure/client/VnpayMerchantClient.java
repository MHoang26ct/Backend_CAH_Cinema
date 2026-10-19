package com.uit.backend_cinema.modules.payment.vnpay.infrastructure.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uit.backend_cinema.common.exception.BusinessException;
import com.uit.backend_cinema.common.exception.ErrorCode;
import com.uit.backend_cinema.common.util.VndMoney;
import com.uit.backend_cinema.modules.payment.vnpay.api.dto.VnpayQueryRequestDTO;
import com.uit.backend_cinema.modules.payment.vnpay.api.dto.VnpayQueryResponseDTO;
import com.uit.backend_cinema.modules.payment.vnpay.api.dto.VnpayRefundRequestDTO;
import com.uit.backend_cinema.modules.payment.vnpay.api.dto.VnpayRefundResponseDTO;
import com.uit.backend_cinema.modules.payment.vnpay.config.VnpayProperties;
import com.uit.backend_cinema.modules.payment.vnpay.domain.util.VnpaySignatureBuilder;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.time.Clock;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class VnpayMerchantClient {

    private static final Logger log = LoggerFactory.getLogger(VnpayMerchantClient.class);
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(VN_ZONE);

    private final VnpayProperties vnpayProperties;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final OkHttpClient httpClient;

    @org.springframework.beans.factory.annotation.Autowired
    public VnpayMerchantClient(VnpayProperties vnpayProperties, ObjectMapper objectMapper, Clock clock) {
        this.vnpayProperties = vnpayProperties;
        this.objectMapper = objectMapper;
        this.clock = clock != null ? clock : Clock.system(VN_ZONE);
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(vnpayProperties.getConnectTimeoutSeconds(), TimeUnit.SECONDS)
                .readTimeout(vnpayProperties.getReadTimeoutSeconds(), TimeUnit.SECONDS)
                .writeTimeout(vnpayProperties.getReadTimeoutSeconds(), TimeUnit.SECONDS)
                // A connection failure can occur after VNPay accepted a refund. Never replay it.
                .retryOnConnectionFailure(false)
                .followRedirects(false)
                .followSslRedirects(false)
                .build();
    }

    public VnpayMerchantClient(VnpayProperties vnpayProperties, ObjectMapper objectMapper, Clock clock, OkHttpClient httpClient) {
        this.vnpayProperties = vnpayProperties;
        this.objectMapper = objectMapper;
        this.clock = clock != null ? clock : Clock.system(VN_ZONE);
        this.httpClient = httpClient;
    }

    /**
     * Truy vấn thông tin giao dịch (querydr) theo VNPay version 2.1.0.
     * This looks up the original payment, not a refund request ID. A signed type-01
     * success response is payment evidence only and must never settle a refund.
     */
    public VnpayQueryResponseDTO queryTransaction(String txnRef, String originalTransactionDate, String orderInfo, String ipAddr) {
        String requestId = generateRequestId();
        String createDate = DATE_FORMATTER.format(clock.instant());
        String info = (orderInfo != null && !orderInfo.isBlank()) ? orderInfo : "Truy van giao dich " + txnRef;
        String ip = (ipAddr != null && !ipAddr.isBlank()) ? ipAddr : "127.0.0.1";

        String hash = VnpaySignatureBuilder.buildQuerydrRequestHash(
                requestId,
                "2.1.0",
                "querydr",
                vnpayProperties.getTmnCode(),
                txnRef,
                originalTransactionDate,
                createDate,
                ip,
                info,
                vnpayProperties.getHashSecret()
        );

        VnpayQueryRequestDTO requestDto = VnpayQueryRequestDTO.builder()
                .requestId(requestId)
                .version("2.1.0")
                .command("querydr")
                .tmnCode(vnpayProperties.getTmnCode())
                .txnRef(txnRef)
                .orderInfo(info)
                .transactionDate(originalTransactionDate)
                .createDate(createDate)
                .ipAddr(ip)
                .secureHash(hash)
                .build();

        log.info("[VNPay Client] Gui querydr: txnRef={}, reqId={}", txnRef, requestId);
        String responseJson = executePost(requestDto, "querydr", txnRef);

        VnpayQueryResponseDTO responseDto;
        try {
            responseDto = objectMapper.readValue(responseJson, VnpayQueryResponseDTO.class);
        } catch (Exception ex) {
            log.error("[VNPay Client] Khong the parse JSON phan hoi querydr cho txnRef={}", txnRef);
            throw new BusinessException("Phản hồi VNPay không hợp lệ", ErrorCode.VNPAY_TRANSACTION_FAILED, ex);
        }

        verifyQuerydrResponse(responseDto, txnRef);
        return responseDto;
    }

    /**
     * Hoàn tiền giao dịch (refund) theo VNPay version 2.1.0.
     *
     * @param txnRef Mã đơn thanh toán gốc
     * @param amountInVnd Số tiền hoàn bằng VND nguyên
     * @param transactionType "02" (hoàn toàn phần) hoặc "03" (hoàn một phần)
     * @param transactionNo Mã GD tại VNPay (nếu có)
     * @param originalTransactionDate Thời gian khởi tạo GD thanh toán gốc (yyyyMMddHHmmss)
     * @param createBy Người/hệ thống yêu cầu hoàn
     * @param orderInfo Mô tả lý do hoàn
     * @param ipAddr IP yêu cầu
     */
    public VnpayRefundResponseDTO refundTransaction(
            String txnRef,
            BigDecimal amountInVnd,
            String transactionType,
            String transactionNo,
            String originalTransactionDate,
            String createBy,
            String orderInfo,
            String ipAddr) {

        if (!"02".equals(transactionType) && !"03".equals(transactionType)) {
            throw new BusinessException("Loại giao dịch hoàn tiền không hợp lệ (phải là 02 hoặc 03)", ErrorCode.VALIDATION_FAILED);
        }

        long gatewayAmount = VndMoney.toGatewayAmount(amountInVnd);
        String requestId = generateRequestId();
        String createDate = DATE_FORMATTER.format(clock.instant());
        String info = (orderInfo != null && !orderInfo.isBlank()) ? orderInfo : "Hoan tien giao dich " + txnRef;
        String ip = (ipAddr != null && !ipAddr.isBlank()) ? ipAddr : "127.0.0.1";
        String transNo = (transactionNo != null && !transactionNo.isBlank()) ? transactionNo : "0";
        String creator = (createBy != null && !createBy.isBlank()) ? createBy : "system";

        String hash = VnpaySignatureBuilder.buildRefundRequestHash(
                requestId,
                "2.1.0",
                "refund",
                vnpayProperties.getTmnCode(),
                transactionType,
                txnRef,
                gatewayAmount,
                transNo,
                originalTransactionDate,
                creator,
                createDate,
                ip,
                info,
                vnpayProperties.getHashSecret()
        );

        VnpayRefundRequestDTO requestDto = VnpayRefundRequestDTO.builder()
                .requestId(requestId)
                .version("2.1.0")
                .command("refund")
                .tmnCode(vnpayProperties.getTmnCode())
                .transactionType(transactionType)
                .txnRef(txnRef)
                .amount(gatewayAmount)
                .transactionNo(transNo)
                .transactionDate(originalTransactionDate)
                .createBy(creator)
                .createDate(createDate)
                .ipAddr(ip)
                .orderInfo(info)
                .secureHash(hash)
                .build();

        log.info("[VNPay Client] Gui refund: txnRef={}, amount={}, type={}, reqId={}", txnRef, amountInVnd, transactionType, requestId);
        String responseJson = executePost(requestDto, "refund", txnRef);

        VnpayRefundResponseDTO responseDto;
        try {
            responseDto = objectMapper.readValue(responseJson, VnpayRefundResponseDTO.class);
        } catch (Exception ex) {
            log.error("[VNPay Client] Khong the parse JSON phan hoi refund cho txnRef={}", txnRef);
            throw new BusinessException("Phản hồi VNPay không hợp lệ", ErrorCode.VNPAY_TRANSACTION_FAILED, ex);
        }

        verifyRefundResponse(responseDto, txnRef, transactionType, gatewayAmount);
        return responseDto;
    }

    private String executePost(Object requestBody, String command, String txnRef) {
        String jsonPayload;
        try {
            jsonPayload = objectMapper.writeValueAsString(requestBody);
        } catch (Exception ex) {
            throw new BusinessException("Không thể serialize request VNPay", ErrorCode.INTERNAL_ERROR, ex);
        }

        RequestBody requestPayload = RequestBody.create(jsonPayload, JSON);
        if ("refund".equals(command)) {
            RequestBody delegate = requestPayload;
            requestPayload = new RequestBody() {
                @Override public MediaType contentType() { return delegate.contentType(); }
                @Override public long contentLength() throws IOException { return delegate.contentLength(); }
                @Override public void writeTo(okio.BufferedSink sink) throws IOException { delegate.writeTo(sink); }
                // Also prevent HTTP follow-ups such as 503/Retry-After: 0 from replaying a refund.
                @Override public boolean isOneShot() { return true; }
            };
        }
        Request request = new Request.Builder()
                .url(vnpayProperties.getApiUrl())
                .post(requestPayload)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String body = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                log.error("[VNPay Client] {} HTTP error: code={}, txnRef={}", command, response.code(), txnRef);
                throw new BusinessException("Lỗi giao tiếp cổng VNPay (HTTP " + response.code() + ")", ErrorCode.GATEWAY_COMMUNICATION_FAILED);
            }
            return body;
        } catch (SocketTimeoutException ex) {
            log.warn("[VNPay Client] {} timeout cho txnRef={}", command, txnRef);
            throw new BusinessException("Cổng VNPay timeout khi gọi " + command, ErrorCode.GATEWAY_TIMEOUT, ex);
        } catch (IOException ex) {
            log.error("[VNPay Client] {} IOException cho txnRef={}: {}", command, txnRef, ex.getMessage());
            throw new BusinessException("Lỗi mạng khi kết nối tới VNPay", ErrorCode.GATEWAY_COMMUNICATION_FAILED, ex);
        }
    }

    private void verifyQuerydrResponse(VnpayQueryResponseDTO responseDto, String expectedTxnRef) {
        boolean validHash = VnpaySignatureBuilder.verifyTransactionResponse(
                responseDto.getSecureHash(),
                responseDto.getResponseId(),
                responseDto.getCommand(),
                responseDto.getResponseCode(),
                responseDto.getMessage(),
                responseDto.getTmnCode(),
                responseDto.getTxnRef(),
                responseDto.getAmount(),
                responseDto.getBankCode(),
                responseDto.getPayDate(),
                responseDto.getTransactionNo(),
                responseDto.getTransactionType(),
                responseDto.getTransactionStatus(),
                responseDto.getOrderInfo(),
                vnpayProperties.getHashSecret()
        );

        if (!validHash) {
            log.warn("[VNPay Client] Chu ky phan hoi querydr khong hop le cho txnRef={}", expectedTxnRef);
            throw new BusinessException("Chữ ký phản hồi VNPay không hợp lệ", ErrorCode.VNPAY_SIGNATURE_INVALID);
        }

        if (responseDto.getTmnCode() != null && !responseDto.getTmnCode().equals(vnpayProperties.getTmnCode())) {
            log.warn("[VNPay Client] TMN Code trong phan hoi khong khop: nhan={}, ky vong={}",
                    responseDto.getTmnCode(), vnpayProperties.getTmnCode());
            throw new BusinessException("TMN Code phản hồi không hợp lệ", ErrorCode.VNPAY_TRANSACTION_FAILED);
        }

        if (responseDto.getTxnRef() != null && !responseDto.getTxnRef().equals(expectedTxnRef)) {
            log.warn("[VNPay Client] TxnRef trong phan hoi khong khop: nhan={}, ky vong={}",
                    responseDto.getTxnRef(), expectedTxnRef);
            throw new BusinessException("Mã tham chiếu giao dịch phản hồi không khớp", ErrorCode.VNPAY_TRANSACTION_FAILED);
        }
    }

    private void verifyRefundResponse(VnpayRefundResponseDTO responseDto, String expectedTxnRef,
                                      String expectedTransactionType, long expectedAmount) {
        boolean validHash = VnpaySignatureBuilder.verifyTransactionResponse(
                responseDto.getSecureHash(),
                responseDto.getResponseId(),
                responseDto.getCommand(),
                responseDto.getResponseCode(),
                responseDto.getMessage(),
                responseDto.getTmnCode(),
                responseDto.getTxnRef(),
                responseDto.getAmount(),
                responseDto.getBankCode(),
                responseDto.getPayDate(),
                responseDto.getTransactionNo(),
                responseDto.getTransactionType(),
                responseDto.getTransactionStatus(),
                responseDto.getOrderInfo(),
                vnpayProperties.getHashSecret()
        );

        if (!validHash) {
            log.warn("[VNPay Client] Chu ky phan hoi refund khong hop le cho txnRef={}", expectedTxnRef);
            throw new BusinessException("Chữ ký phản hồi VNPay không hợp lệ", ErrorCode.VNPAY_SIGNATURE_INVALID);
        }

        if (responseDto.getTmnCode() != null && !responseDto.getTmnCode().equals(vnpayProperties.getTmnCode())) {
            log.warn("[VNPay Client] TMN Code trong phan hoi refund khong khop");
            throw new BusinessException("TMN Code phản hồi không hợp lệ", ErrorCode.VNPAY_TRANSACTION_FAILED);
        }

        if (!expectedTxnRef.equals(responseDto.getTxnRef())) {
            log.warn("[VNPay Client] TxnRef trong phan hoi refund khong khop");
            throw new BusinessException("Mã tham chiếu giao dịch phản hồi không khớp", ErrorCode.VNPAY_TRANSACTION_FAILED);
        }
        if ((responseDto.getCommand() != null && !"refund".equals(responseDto.getCommand()))
                || ("00".equals(responseDto.getResponseCode())
                && (!expectedTransactionType.equals(responseDto.getTransactionType())
                || !Long.valueOf(expectedAmount).equals(responseDto.getAmount())))) {
            // Authentication alone is not enough: a signed original-payment response is
            // not evidence that the requested full/partial refund has been processed.
            throw new BusinessException("Phản hồi không khớp yêu cầu hoàn tiền VNPay", ErrorCode.VNPAY_TRANSACTION_FAILED);
        }
    }

    private String generateRequestId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 32);
    }
}
