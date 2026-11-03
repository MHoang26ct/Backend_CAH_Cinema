package com.uit.backend_cinema.unit_test;

import com.uit.backend_cinema.common.sercurity.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.DefaultCorsProcessor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SecurityCorsConfigTest {
    private CorsConfiguration configuration(List<String> origins, MockHttpServletRequest request) {
        SecurityConfig config = new SecurityConfig(null, null, null, null);
        ReflectionTestUtils.setField(config, "allowedOrigins", origins);
        return config.corsConfigurationSource().getCorsConfiguration(request);
    }

    private MockHttpServletRequest preflight(String origin, String method, String headers) {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/v1/bookings");
        request.addHeader("Origin", origin);
        request.addHeader("Access-Control-Request-Method", method);
        request.addHeader("Access-Control-Request-Headers", headers);
        return request;
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://mhoang26ct.online", "http://localhost:5173"})
    void allowsFrontendAndLocalDevelopment(String origin) throws Exception {
        MockHttpServletRequest request = preflight(origin, "POST", "authorization,content-type");
        MockHttpServletResponse response = new MockHttpServletResponse();
        CorsConfiguration cors = configuration(List.of("https://mhoang26ct.online", "http://localhost:5173"), request);

        assertTrue(new DefaultCorsProcessor().processRequest(cors, request, response));
        assertEquals(origin, response.getHeader("Access-Control-Allow-Origin"));
        assertNull(response.getHeader("Access-Control-Allow-Credentials"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://cors-check.example", "https://mhoang26ct.online.evil.example",
            "http://mhoang26ct.online", "http://localhost:5174"
    })
    void rejectsOriginsOutsideAllowlist(String origin) throws Exception {
        MockHttpServletRequest request = preflight(origin, "GET", "authorization");
        MockHttpServletResponse response = new MockHttpServletResponse();
        CorsConfiguration cors = configuration(List.of("https://mhoang26ct.online", "http://localhost:5173"), request);

        assertFalse(new DefaultCorsProcessor().processRequest(cors, request, response));
        assertEquals(403, response.getStatus());
        assertNull(response.getHeader("Access-Control-Allow-Origin"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"})
    void preservesAllowedApiMethods(String method) throws Exception {
        MockHttpServletRequest request = preflight("https://mhoang26ct.online", method, "authorization,content-type,accept");
        MockHttpServletResponse response = new MockHttpServletResponse();
        CorsConfiguration cors = configuration(List.of("https://mhoang26ct.online"), request);

        assertTrue(new DefaultCorsProcessor().processRequest(cors, request, response));
    }

    @Test
    void usesConfiguredOriginsInsteadOfHardcodedWildcard() throws Exception {
        MockHttpServletRequest request = preflight("https://preview.example", "GET", "authorization");
        MockHttpServletResponse response = new MockHttpServletResponse();
        CorsConfiguration cors = configuration(List.of("https://preview.example"), request);

        assertTrue(new DefaultCorsProcessor().processRequest(cors, request, response));
        assertEquals("https://preview.example", response.getHeader("Access-Control-Allow-Origin"));
        assertNull(cors.checkOrigin("https://mhoang26ct.online"));
        assertEquals(Boolean.FALSE, cors.getAllowCredentials());
    }

    @Test
    void rejectsUnapprovedRequestHeader() throws Exception {
        MockHttpServletRequest request = preflight("https://mhoang26ct.online", "GET", "x-unapproved-header");
        MockHttpServletResponse response = new MockHttpServletResponse();
        CorsConfiguration cors = configuration(List.of("https://mhoang26ct.online"), request);

        assertFalse(new DefaultCorsProcessor().processRequest(cors, request, response));
        assertEquals(403, response.getStatus());
    }

    @Test
    void doesNotRejectServerToServerWebhookWithoutOrigin() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/public/stripe/webhook");
        MockHttpServletResponse response = new MockHttpServletResponse();
        CorsConfiguration cors = configuration(List.of("https://mhoang26ct.online"), request);

        assertTrue(new DefaultCorsProcessor().processRequest(cors, request, response));
        assertNull(response.getHeader("Access-Control-Allow-Origin"));
    }
}
