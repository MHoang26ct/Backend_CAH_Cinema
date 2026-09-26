package com.uit.backend_cinema.integration_test;

import com.uit.backend_cinema.common.exception.GlobalExceptionHandler;
import com.uit.backend_cinema.common.util.JwtUtil;
import com.uit.backend_cinema.modules.auth.api.controller.AuthController;
import com.uit.backend_cinema.modules.auth.api.mapper.UserApiMapper;
import com.uit.backend_cinema.modules.auth.application.AuthUseCase;
import com.uit.backend_cinema.modules.auth.domain.entity.User;
import com.uit.backend_cinema.modules.auth.domain.repository.UserRepository;
import com.uit.backend_cinema.modules.auth.domain.service.AuthService;
import com.uit.backend_cinema.modules.auth.domain.service.RefreshTokenService;
import com.uit.backend_cinema.modules.auth.infrastructure.security.CustomUserDetailsService;
import com.uit.backend_cinema.modules.notification.domain.repository.EmailSender;
import com.uit.backend_cinema.modules.notification.domain.service.NotificationService;
import com.uit.backend_cinema.modules.notification.infrastructure.persistence.RedisOtpStorageImpl;
import org.junit.jupiter.api.*;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers(disabledWithoutDocker = true)
class RegistrationOtpIntegrationTest {
    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);
    static LettuceConnectionFactory factory;
    static RedisTemplate<Object, Object> redis;
    RedisOtpStorageImpl storage;
    EmailSender emailSender;
    UserRepository users;
    JwtUtil jwt;
    RefreshTokenService refreshTokens;
    MockMvc mvc;
    String email;
    String deliveredOtp;

    @BeforeAll
    static void connect() {
        factory = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
        factory.afterPropertiesSet();
        redis = new RedisTemplate<>();
        redis.setConnectionFactory(factory);
        redis.afterPropertiesSet(); // Same default serializers as the application's RedisTemplate.
    }

    @AfterAll
    static void close() {
        if (factory != null) factory.destroy();
    }

    @BeforeEach
    void setup() {
        email = UUID.randomUUID() + "@example.com";
        storage = new RedisOtpStorageImpl(redis);
        emailSender = mock(EmailSender.class);
        users = mock(UserRepository.class);
        jwt = mock(JwtUtil.class);
        refreshTokens = mock(RefreshTokenService.class);
        var details = mock(CustomUserDetailsService.class);
        var mapper = mock(UserApiMapper.class);
        var notifications = new NotificationService(emailSender, storage, jwt);
        var auth = new AuthService(users, new BCryptPasswordEncoder(), notifications);
        var useCase = new AuthUseCase(auth, refreshTokens, details, jwt, mapper);
        mvc = MockMvcBuilders.standaloneSetup(new AuthController(useCase, notifications))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        doAnswer(call -> {
            var matcher = Pattern.compile("[0-9]{6}").matcher(call.getArgument(2, String.class));
            assertTrue(matcher.find());
            deliveredOtp = matcher.group();
            return null;
        }).when(emailSender).sendEmail(eq(email), anyString(), anyString());
        when(users.save(any(User.class))).thenAnswer(call -> {
            User user = call.getArgument(0);
            user.setUserId(123L);
            return user;
        });
        var principal = org.springframework.security.core.userdetails.User
                .withUsername(email).password("encoded").roles("USER").build();
        when(details.loadUserById(123L)).thenReturn(principal);
        when(jwt.generateToken(anyMap(), eq(principal))).thenReturn("access-token");
        when(refreshTokens.createRefreshToken(123L)).thenReturn("refresh-token");
    }

    void send() throws Exception {
        mvc.perform(post("/api/v1/auth/register/send-otp").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\"}"))
                .andExpect(status().isOk());
    }

    org.springframework.test.web.servlet.ResultActions register(String otp) throws Exception {
        return mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"secret\",\"name\":\"Test\""
                        + (otp == null ? "" : ",\"otp\":\"" + otp + "\"") + "}"));
    }

    @Test
    void verifiesDeliveredEmailBeforeCreatingAccountAndTokensAndRejectsReplay() throws Exception {
        send();
        verify(users, never()).save(any());
        verifyNoInteractions(refreshTokens);
        register(deliveredOtp).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"));
        verify(users).save(argThat(user -> email.equals(user.getEmail())
                && new BCryptPasswordEncoder().matches("secret", user.getPassword())));
        register(deliveredOtp).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("OTP_INVALID"));
        verify(users, times(1)).save(any());
        verify(refreshTokens, times(1)).createRefreshToken(123L);
    }

    @Test
    void missingMalformedWrongExpiredAndOtherPurposeOtpCannotCreateAccount() throws Exception {
        register(null).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        register("abc123").andExpect(status().isBadRequest());
        register("123456").andExpect(status().isBadRequest());
        storage.save("OTP: " + email, "123456", 5);
        register("123456").andExpect(status().isBadRequest());
        send();
        register(deliveredOtp.equals("123456") ? "654321" : "123456").andExpect(status().isBadRequest());
        redis.expire("OTP:REGISTER:" + email, Duration.ZERO);
        register(deliveredOtp).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("OTP_INVALID"));
        verify(users, never()).save(any());
        verifyNoInteractions(refreshTokens, jwt);
    }

    @Test
    void bindsOtpToEmailAndSeparatesPasswordReset() throws Exception {
        send();
        assertFalse(storage.consumeRegistrationOtp("other@example.com", deliveredOtp));
        mvc.perform(post("/api/v1/auth/fp-verify-otp").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"otp\":\"" + deliveredOtp + "\"}"))
                .andExpect(status().isBadRequest());
        register(deliveredOtp).andExpect(status().isOk());
    }

    @Test
    void limitsResendsAndReplacesPreviousCodeAfterCooldown() throws Exception {
        send();
        String first = deliveredOtp;
        Long ttl = redis.getExpire("OTP:REGISTER:" + email, TimeUnit.SECONDS);
        assertNotNull(ttl);
        assertTrue(ttl > 290 && ttl <= 300);
        mvc.perform(post("/api/v1/auth/register/send-otp").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\"}"))
                .andExpect(status().isTooManyRequests()).andExpect(jsonPath("$.code").value("OTP_RATE_LIMITED"));
        verify(emailSender, times(1)).sendEmail(anyString(), anyString(), anyString());
        redis.delete("OTP:REGISTER:" + email + ":cooldown");
        String replacement = first.equals("123456") ? "654321" : "123456";
        assertTrue(storage.saveRegistrationOtp(email, replacement));
        assertFalse(storage.consumeRegistrationOtp(email, first));
        assertTrue(storage.consumeRegistrationOtp(email, replacement));
    }

    @Test
    void domainCaseVariantsShareCooldownAndVerification() throws Exception {
        send();
        email = email.replace("@example.com", "@EXAMPLE.COM");
        mvc.perform(post("/api/v1/auth/register/send-otp").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("OTP_RATE_LIMITED"));
        verify(emailSender, times(1)).sendEmail(anyString(), anyString(), anyString());
        register(deliveredOtp).andExpect(status().isOk());
        assertFalse(storage.consumeRegistrationOtp(email.replace("@EXAMPLE.COM", "@example.com"), deliveredOtp));
    }

    @Test
    void invalidatesCodeAfterFiveWrongAttempts() throws Exception {
        send();
        String wrong = deliveredOtp.equals("123456") ? "654321" : "123456";
        for (int i = 0; i < 5; i++) register(wrong).andExpect(status().isBadRequest());
        register(deliveredOtp).andExpect(status().isBadRequest());
        verify(users, never()).save(any());
    }

    @Test
    void rejectsExistingEmailBeforeSendingOrConsumingOtp() throws Exception {
        send();
        when(users.existsByEmail(email)).thenReturn(true);
        mvc.perform(post("/api/v1/auth/register/send-otp").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\"}"))
                .andExpect(status().isConflict());
        register(deliveredOtp).andExpect(status().isConflict());
        assertTrue(storage.consumeRegistrationOtp(email, deliveredOtp));
        verify(users, never()).save(any());
        verify(emailSender, times(1)).sendEmail(anyString(), anyString(), anyString());
    }

    @Test
    void concurrentVerificationConsumesCodeOnlyOnce() throws Exception {
        send();
        var executor = Executors.newFixedThreadPool(2);
        try {
            Callable<Boolean> consume = () -> storage.consumeRegistrationOtp(email, deliveredOtp);
            var results = executor.invokeAll(List.of(consume, consume));
            assertEquals(1, (results.get(0).get() ? 1 : 0) + (results.get(1).get() ? 1 : 0));
        } finally {
            executor.shutdownNow();
        }
    }
}
