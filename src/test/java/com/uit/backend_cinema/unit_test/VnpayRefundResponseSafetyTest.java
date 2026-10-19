package com.uit.backend_cinema.unit_test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uit.backend_cinema.common.exception.BusinessException;
import com.uit.backend_cinema.common.exception.ErrorCode;
import com.uit.backend_cinema.modules.payment.vnpay.api.dto.VnpayRefundResponseDTO;
import com.uit.backend_cinema.modules.payment.vnpay.config.VnpayProperties;
import com.uit.backend_cinema.modules.payment.vnpay.domain.util.VnpaySignatureBuilder;
import com.uit.backend_cinema.modules.payment.vnpay.infrastructure.client.VnpayMerchantClient;
import okhttp3.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class VnpayRefundResponseSafetyTest {
    private static final String SECRET = "test-only-secret";
    private final ObjectMapper mapper = new ObjectMapper();
    private final OkHttpClient http = mock(OkHttpClient.class);
    private final Call call = mock(Call.class);
    private VnpayProperties properties;
    private VnpayMerchantClient client;

    @BeforeEach
    void setUp() {
        properties = new VnpayProperties();
        properties.setTmnCode("TESTTMN1");
        properties.setHashSecret(SECRET);
        properties.setApiUrl("https://vnpay.invalid/refund");
        client = new VnpayMerchantClient(properties, mapper, Clock.systemUTC(), http);
        when(http.newCall(any())).thenReturn(call);
    }

    @ParameterizedTest
    @CsvSource({"querydr,01,5000000", "refund,01,5000000", "refund,02,5000000", "refund,03,10000000"})
    void evenAuthenticallySignedPaymentOrWrongRefundCannotSettleRefund(String command, String type, long amount) throws Exception {
        VnpayRefundResponseDTO response = response(command, type, amount);
        reply(response);
        BusinessException error = assertThrows(BusinessException.class, this::refund);
        assertEquals(ErrorCode.VNPAY_TRANSACTION_FAILED, error.getCode());
        verify(call, times(1)).execute();
    }

    @Test
    void signedResponseWithMissingOriginalReferenceIsRejected() throws Exception {
        VnpayRefundResponseDTO response = response("refund", "03", 5000000L);
        response.setTxnRef(null);
        reply(response);
        assertEquals(ErrorCode.VNPAY_TRANSACTION_FAILED,
                assertThrows(BusinessException.class, this::refund).getCode());
    }

    @Test
    void matchingSignedRefundSuccessIsAccepted() throws Exception {
        reply(response("refund", "03", 5000000L));
        assertTrue(refund().isRefundSucceeded());
    }

    @Test
    void matchingSignedPendingRefundIsNotSuccess() throws Exception {
        VnpayRefundResponseDTO response = response("refund", "03", 5000000L);
        response.setTransactionStatus("06");
        reply(response);
        VnpayRefundResponseDTO result = refund();
        assertTrue(result.isRefundProcessing());
        assertFalse(result.isRefundSucceeded());
    }

    @Test
    void defaultTransportDoesNotReplayRefundsOnConnectionFailureOrRedirect() {
        VnpayMerchantClient production = new VnpayMerchantClient(properties, mapper, Clock.systemUTC());
        OkHttpClient transport = (OkHttpClient) ReflectionTestUtils.getField(production, "httpClient");
        assertNotNull(transport);
        assertFalse(transport.retryOnConnectionFailure());
        assertFalse(transport.followRedirects());
        assertFalse(transport.followSslRedirects());
    }

    private VnpayRefundResponseDTO refund() {
        return client.refundTransaction("ORDER_001", new BigDecimal("50000"), "03", "PAYMENT_001",
                "20261019090000", "system", "Refund booking", "127.0.0.1");
    }

    private VnpayRefundResponseDTO response(String command, String type, long amount) {
        return VnpayRefundResponseDTO.builder().responseId("RESPONSE_001").command(command)
                .responseCode("00").message("Success").tmnCode("TESTTMN1").txnRef("ORDER_001")
                .amount(amount).bankCode("NCB").payDate("20261019100000").transactionNo("123456789")
                .transactionType(type).transactionStatus("00").orderInfo("Refund booking").build();
    }

    private void reply(VnpayRefundResponseDTO dto) throws Exception {
        dto.setSecureHash(VnpaySignatureBuilder.buildTransactionResponseHash(dto.getResponseId(), dto.getCommand(),
                dto.getResponseCode(), dto.getMessage(), dto.getTmnCode(), dto.getTxnRef(), dto.getAmount(),
                dto.getBankCode(), dto.getPayDate(), dto.getTransactionNo(), dto.getTransactionType(),
                dto.getTransactionStatus(), dto.getOrderInfo(), SECRET));
        when(call.execute()).thenReturn(new Response.Builder()
                .request(new Request.Builder().url(properties.getApiUrl()).build())
                .protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .body(ResponseBody.create(mapper.writeValueAsString(dto), MediaType.get("application/json"))).build());
    }
}
