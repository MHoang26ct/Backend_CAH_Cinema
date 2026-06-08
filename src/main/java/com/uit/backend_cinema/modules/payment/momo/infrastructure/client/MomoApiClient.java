package com.uit.backend_cinema.modules.payment.momo.infrastructure.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uit.backend_cinema.common.exception.BusinessException;
import com.uit.backend_cinema.common.exception.ErrorCode;
import com.uit.backend_cinema.modules.payment.momo.config.MomoProperties;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * HTTP client gọi MoMo Payment API.
 * Sử dụng OkHttp với timeout 30s theo yêu cầu của MoMo.
 */
@Component
public class MomoApiClient {

    private static final Logger log = LoggerFactory.getLogger(MomoApiClient.class);
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final MomoProperties momoProperties;

    public MomoApiClient(MomoProperties momoProperties, ObjectMapper objectMapper) {
        this.momoProperties = momoProperties;
        this.objectMapper = objectMapper;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    /**
     * Gọi MoMo POST /v2/gateway/api/create để tạo đơn thanh toán.
     *
     * @param requestBody map chứa các field của request
     * @return JsonNode chứa response từ MoMo
     */
    public JsonNode createPaymentOrder(Object requestBody) {
        try {
            String json = objectMapper.writeValueAsString(requestBody);
            log.debug("[MoMo] POST {} | body: {}", momoProperties.getCreateUrl(), json);

            Request request = new Request.Builder()
                    .url(momoProperties.getCreateUrl())
                    .post(RequestBody.create(json, JSON))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                log.debug("[MoMo] Response status: {} | body: {}", response.code(), responseBody);

                if (!response.isSuccessful()) {
                    log.error("[MoMo] HTTP error {} khi tạo đơn: {}", response.code(), responseBody);
                    throw new BusinessException(
                            "Gọi MoMo API thất bại, HTTP status: " + response.code(),
                            ErrorCode.MOMO_PAYMENT_CREATION_FAILED);
                }

                return objectMapper.readTree(responseBody);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            log.error("[MoMo] IOException khi gọi API tạo đơn", e);
            throw new BusinessException("Không thể kết nối MoMo API", ErrorCode.MOMO_PAYMENT_CREATION_FAILED);
        }
    }
}
