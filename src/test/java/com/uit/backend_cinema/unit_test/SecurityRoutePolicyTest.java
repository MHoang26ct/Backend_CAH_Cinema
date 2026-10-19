package com.uit.backend_cinema.unit_test;

import java.util.List;
import java.util.Map;

import com.uit.backend_cinema.common.sercurity.CustomAccessDeniedHandler;
import com.uit.backend_cinema.common.sercurity.CustomAuthenticationEntryPoint;
import com.uit.backend_cinema.common.sercurity.CustomUserDetails;
import com.uit.backend_cinema.common.sercurity.JwtAuthenticationFilter;
import com.uit.backend_cinema.common.sercurity.SecurityConfig;
import com.uit.backend_cinema.common.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Real security chain, stub endpoints/account store; no DB, Redis, or application bootstrap. */
@SpringJUnitConfig(SecurityRoutePolicyTest.Config.class)
@WebAppConfiguration
@TestPropertySource(properties = {
        "jwt.secret=offline-test-key-only-at-least-32-bytes-long", "jwt.expiration=60000"
})
class SecurityRoutePolicyTest {
    @Autowired WebApplicationContext context;
    @Autowired JwtUtil jwt;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/staff/bookings", "/api/v1/staff/bookings/1/confirm-payment",
            "/api/v1/staff/bookings/1/cancel", "/api/v1/staff/refunds/1/claim",
            "/api/v1/staff/refunds/1/confirm-cash-payout", "/api/v1/staff/check-in"})
    void customerCannotCallStaffRoutes(String path) throws Exception {
        mvc.perform(post(path).header("Authorization", bearer("USER"))).andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @CsvSource({"STAFF, 200", "ADMIN, 200", "USER, 403"})
    void staffNamespaceAllowsOnlyStaffAndAdmin(String role, int expected) throws Exception {
        mvc.perform(post("/api/v1/staff/bookings").header("Authorization", bearer(role)))
                .andExpect(status().is(expected));
    }

    @ParameterizedTest
    @CsvSource({"STAFF, 403", "USER, 403", "ADMIN, 200"})
    void staffCannotUseAdminCrud(String role, int expected) throws Exception {
        mvc.perform(post("/api/v1/admin/movies").header("Authorization", bearer(role)))
                .andExpect(status().is(expected));
    }

    @Test
    void anonymousCannotUseStaffOrCustomerRoutes() throws Exception {
        mvc.perform(post("/api/v1/staff/bookings")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/bookings")).andExpect(status().isUnauthorized());
    }

    @Test
    void resetAndQrDoNotAuthenticateButPublicRoutesRemainAccessible() throws Exception {
        String reset = jwt.generateResetToken(principal("ADMIN"));
        String qr = jwt.generateTicketQrToken(1L, 2L, 3L);
        for (String token : List.of(reset, qr)) {
            mvc.perform(post("/api/v1/staff/bookings").header("Authorization", "Bearer " + token))
                    .andExpect(status().isUnauthorized());
            mvc.perform(post("/api/v1/auth/change-password").header("Authorization", "Bearer " + token))
                    .andExpect(status().isUnauthorized());
            mvc.perform(get("/api/v1/public/probe").header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void customerAccessCanReachCustomerRoute() throws Exception {
        mvc.perform(post("/api/v1/bookings").header("Authorization", bearer("USER")))
                .andExpect(status().isOk());
    }

    private String bearer(String role) {
        return "Bearer " + jwt.generateToken(Map.of("userId", 7L), principal(role));
    }

    private static CustomUserDetails principal(String role) {
        return new CustomUserDetails(7L, role + "@example.com", "encoded",
                List.of(new SimpleGrantedAuthority("ROLE_" + role)));
    }

    @Configuration
    @EnableWebMvc
    @Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtUtil.class,
            CustomAuthenticationEntryPoint.class, CustomAccessDeniedHandler.class, ProbeController.class})
    static class Config {
        @Bean
        UserDetailsService userDetailsService() {
            return email -> {
                for (String role : List.of("USER", "STAFF", "ADMIN")) {
                    if (principal(role).getUsername().equals(email)) return principal(role);
                }
                throw new UsernameNotFoundException("Unknown account");
            };
        }
    }

    @RestController
    static class ProbeController {
        @RequestMapping({"/api/v1/staff/**", "/api/v1/admin/**", "/api/v1/bookings",
                "/api/v1/auth/change-password", "/api/v1/public/probe"})
        String probe() {
            return "ok";
        }
    }
}
