package com.uit.backend_cinema.unit_test;

import com.uit.backend_cinema.common.exception.GlobalExceptionHandler;
import com.uit.backend_cinema.modules.payment.stripe.api.*;
import com.uit.backend_cinema.modules.payment.stripe.config.StripeSandboxConfig;
import com.uit.backend_cinema.modules.payment.stripe.domain.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StripeSandboxBoundaryTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(StripeSandboxController.class)
            .withBean(StripePaymentService.class, () -> mock(StripePaymentService.class))
            .withBean(StripeSandboxConfig.class, () -> mock(StripeSandboxConfig.class));

    @Test void driverAbsentByDefault() { runner.run(c -> assertThat(c).doesNotHaveBean(StripeSandboxController.class)); }
    @Test void flagsAloneDoNotEnableDriver() {
        runner.withPropertyValues("stripe.enabled=true", "stripe.sandbox-driver-enabled=true")
                .run(c -> assertThat(c).doesNotHaveBean(StripeSandboxController.class));
    }
    @Test void sandboxProfileWithoutFlagDoesNotEnableDriver() {
        runner.withPropertyValues("spring.profiles.active=stripe-sandbox", "stripe.enabled=true")
                .run(c -> assertThat(c).doesNotHaveBean(StripeSandboxController.class));
    }
    @Test void driverExistsOnlyWhenProfileAndFlagsMatch() {
        runner.withPropertyValues("spring.profiles.active=stripe-sandbox", "stripe.enabled=true", "stripe.sandbox-driver-enabled=true")
                .run(c -> assertThat(c).hasSingleBean(StripeSandboxController.class));
    }
    @Test void productionProfileOverridesSandboxProfile() {
        runner.withPropertyValues("spring.profiles.active=stripe-sandbox,production", "stripe.enabled=true", "stripe.sandbox-driver-enabled=true")
                .run(c -> assertThat(c).doesNotHaveBean(StripeSandboxController.class));
    }
    @Test void webhookSignatureErrorsAre400NotGlobalHandler500() throws Exception {
        var payments=mock(StripePaymentService.class); var webhooks=mock(StripeWebhookService.class);
        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid Stripe event/signature"))
                .when(webhooks).handle(anyString(),nullable(String.class));
        var mvc=MockMvcBuilders.standaloneSetup(new StripePaymentController(payments,webhooks))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        mvc.perform(post("/api/v1/public/stripe/webhook").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/public/stripe/webhook").header("Stripe-Signature","invalid").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
    }
}
