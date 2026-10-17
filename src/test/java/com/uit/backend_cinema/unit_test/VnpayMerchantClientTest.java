package com.uit.backend_cinema.unit_test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uit.backend_cinema.common.exception.BusinessException;
import com.uit.backend_cinema.common.exception.ErrorCode;
import com.uit.backend_cinema.modules.payment.vnpay.api.dto.VnpayQueryResponseDTO;
import com.uit.backend_cinema.modules.payment.vnpay.api.dto.VnpayRefundResponseDTO;
import com.uit.backend_cinema.modules.payment.vnpay.config.VnpayProperties;
import com.uit.backend_cinema.modules.payment.vnpay.domain.util.VnpaySignatureBuilder;
import com.uit.backend_cinema.modules.payment.vnpay.infrastructure.client.VnpayMerchantClient;
import okhttp3.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class VnpayMerchantClientTest {

    private final String tmnCode = "TESTTMN1";
    private final String hashSecret = "SECRETKEY1234567890ABCDEF";
    private final ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");
    private final Clock fixedClock = Clock.fixed(Instant.parse("2026-10-17T08:30:00Z"), zone);

    private VnpayProperties vnpayProperties;
    private ObjectMapper objectMapper;
    private OkHttpClient mockHttpClient;
    private Call mockCall;
    private VnpayMerchantClient client;

    @BeforeEach
    void setUp() {
        vnpayProperties = new VnpayProperties();
        vnpayProperties.setTmnCode(tmnCode);
        vnpayProperties.setHashSecret(hashSecret);
        vnpayProperties.setApiUrl("https://sandbox.vnpayment.vn/merchant_webapi/api/transaction");

        objectMapper = new ObjectMapper();
        mockHttpClient = mock(OkHttpClient.class);
        mockCall = mock(Call.class);
        when(mockHttpClient.newCall(any(Request.class))).thenReturn(mockCall);

        client = new VnpayMerchantClient(vnpayProperties, objectMapper, fixedClock, mockHttpClient);
    }

    @Test
    @DisplayName("Querydr request: sinh chữ ký chuẩn pipe-delimited và verify response thành công")
    void testQuerydrSuccess() throws Exception {
        String txnRef = "1001_1729152000000";
        String origDate = "20261017150000";

        // Build valid response JSON
        String responseId = "RES_001";
        String command = "querydr";
        String responseCode = "00";
        String message = "Query success";
        long amount = 10000000L;
        String bankCode = "NCB";
        String payDate = "20261017150500";
        String transNo = "14567890";
        String transType = "01";
        String transStatus = "00";
        String orderInfo = "Truy van giao dich " + txnRef;

        String expectedHash = VnpaySignatureBuilder.buildTransactionResponseHash(
                responseId, command, responseCode, message, tmnCode, txnRef,
                amount, bankCode, payDate, transNo, transType, transStatus, orderInfo, hashSecret);

        VnpayQueryResponseDTO responseDto = VnpayQueryResponseDTO.builder()
                .responseId(responseId)
                .command(command)
                .responseCode(responseCode)
                .message(message)
                .tmnCode(tmnCode)
                .txnRef(txnRef)
                .amount(amount)
                .bankCode(bankCode)
                .payDate(payDate)
                .transactionNo(transNo)
                .transactionType(transType)
                .transactionStatus(transStatus)
                .orderInfo(orderInfo)
                .secureHash(expectedHash)
                .build();

        String responseJson = objectMapper.writeValueAsString(responseDto);

        Response response = new Response.Builder()
                .request(new Request.Builder().url(vnpayProperties.getApiUrl()).build())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(ResponseBody.create(responseJson, MediaType.get("application/json")))
                .build();

        when(mockCall.execute()).thenReturn(response);

        VnpayQueryResponseDTO result = client.queryTransaction(txnRef, origDate, orderInfo, "127.0.0.1");

        assertNotNull(result);
        assertEquals("00", result.getResponseCode());
        assertEquals("00", result.getTransactionStatus());
        assertTrue(result.isPaymentSuccess());
        assertEquals(amount, result.getAmount());
    }

    @Test
    @DisplayName("Querydr response: chữ ký sai bị từ chối với VNPAY_SIGNATURE_INVALID")
    void testQuerydrInvalidSignatureRejected() throws Exception {
        String txnRef = "1001_1729152000000";
        String origDate = "20261017150000";

        VnpayQueryResponseDTO responseDto = VnpayQueryResponseDTO.builder()
                .responseId("RES_001")
                .command("querydr")
                .responseCode("00")
                .message("Tampered")
                .tmnCode(tmnCode)
                .txnRef(txnRef)
                .amount(10000000L)
                .transactionStatus("00")
                .secureHash("INVALID_SIGNATURE_HASH")
                .build();

        Response response = new Response.Builder()
                .request(new Request.Builder().url(vnpayProperties.getApiUrl()).build())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(ResponseBody.create(objectMapper.writeValueAsString(responseDto), MediaType.get("application/json")))
                .build();

        when(mockCall.execute()).thenReturn(response);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> client.queryTransaction(txnRef, origDate, "info", "127.0.0.1"));
        assertEquals(ErrorCode.VNPAY_SIGNATURE_INVALID, ex.getCode());
    }

    @Test
    @DisplayName("Querydr timeout: ném GATEWAY_TIMEOUT, không tự ý giả định kết quả")
    void testQuerydrTimeoutThrowsGatewayTimeout() throws Exception {
        when(mockCall.execute()).thenThrow(new SocketTimeoutException("Read timed out"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> client.queryTransaction("1001", "20261017150000", "info", "127.0.0.1"));
        assertEquals(ErrorCode.GATEWAY_TIMEOUT, ex.getCode());
    }

    @Test
    @DisplayName("Refund request: loại hoàn tiền 02/03 hợp lệ, chuyển đổi tiền VND*100 chính xác")
    void testRefundSuccess() throws Exception {
        String txnRef = "1001_1729152000000";
        BigDecimal refundAmountVnd = new BigDecimal("50000"); // 50,000 VND
        long expectedGatewayAmount = 5000000L; // 50,000 * 100

        String responseId = "REFUND_RES_001";
        String command = "refund";
        String responseCode = "00";
        String message = "Refund requested successfully";
        String bankCode = "NCB";
        String payDate = "20261017153000";
        String transNo = "99887766";
        String transType = "03"; // partial
        String transStatus = "00"; // success
        String orderInfo = "Hoan ve huy suat chieu";

        String expectedHash = VnpaySignatureBuilder.buildTransactionResponseHash(
                responseId, command, responseCode, message, tmnCode, txnRef,
                expectedGatewayAmount, bankCode, payDate, transNo, transType, transStatus, orderInfo, hashSecret);

        VnpayRefundResponseDTO responseDto = VnpayRefundResponseDTO.builder()
                .responseId(responseId)
                .command(command)
                .responseCode(responseCode)
                .message(message)
                .tmnCode(tmnCode)
                .txnRef(txnRef)
                .amount(expectedGatewayAmount)
                .bankCode(bankCode)
                .payDate(payDate)
                .transactionNo(transNo)
                .transactionType(transType)
                .transactionStatus(transStatus)
                .orderInfo(orderInfo)
                .secureHash(expectedHash)
                .build();

        Response response = new Response.Builder()
                .request(new Request.Builder().url(vnpayProperties.getApiUrl()).build())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(ResponseBody.create(objectMapper.writeValueAsString(responseDto), MediaType.get("application/json")))
                .build();

        when(mockCall.execute()).thenReturn(response);

        VnpayRefundResponseDTO result = client.refundTransaction(
                txnRef, refundAmountVnd, "03", "14567890", "20261017150000", "staff1", orderInfo, "127.0.0.1");

        assertNotNull(result);
        assertEquals("00", result.getResponseCode());
        assertEquals("00", result.getTransactionStatus());
        assertTrue(result.isRefundSucceeded());
        assertEquals(expectedGatewayAmount, result.getAmount());
    }

    @Test
    @DisplayName("Refund request: loại hoàn không phải 02/03 bị từ chối với VALIDATION_FAILED")
    void testRefundInvalidTypeRejected() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> client.refundTransaction("1001", new BigDecimal("10000"), "01", "0",
                        "20261017150000", "staff", "info", "127.0.0.1"));
        assertEquals(ErrorCode.VALIDATION_FAILED, ex.getCode());
    }

    @Test
    @DisplayName("Refund timeout: ném GATEWAY_TIMEOUT, bảo vệ không tự retry vô điều kiện")
    void testRefundTimeoutThrowsGatewayTimeout() throws Exception {
        when(mockCall.execute()).thenThrow(new SocketTimeoutException("Read timed out"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> client.refundTransaction("1001", new BigDecimal("10000"), "02", "0",
                        "20261017150000", "staff", "info", "127.0.0.1"));
        assertEquals(ErrorCode.GATEWAY_TIMEOUT, ex.getCode());
    }
}
