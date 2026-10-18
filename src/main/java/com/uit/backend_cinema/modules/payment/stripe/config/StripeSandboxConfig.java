package com.uit.backend_cinema.modules.payment.stripe.config;

import com.stripe.StripeClient;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;

@Configuration
@ConditionalOnProperty(name = "stripe.enabled", havingValue = "true")
@ConfigurationProperties(prefix = "stripe")
@Getter
@Setter
public class StripeSandboxConfig {
    private String secretKey;
    private String webhookSecret;
    private String accountId;
    private String successUrl;
    private String cancelUrl;
    private boolean testMode = true;
    private boolean sandboxDriverEnabled;
    private String sandboxToken;

    @PostConstruct
    public void validate() {
        if (!testMode || secretKey == null || !secretKey.startsWith("sk_test_") ||
                webhookSecret == null || !webhookSecret.startsWith("whsec_") ||
                accountId == null || !accountId.startsWith("acct_")) {
            throw new IllegalStateException("Stripe sandbox requires test key, signing secret and account ID");
        }
        if (sandboxDriverEnabled && (sandboxToken == null || sandboxToken.length() < 32)) {
            throw new IllegalStateException("Stripe automation requires a dedicated token of at least 32 characters");
        }
        validateUrl(successUrl);
        validateUrl(cancelUrl);
    }

    private void validateUrl(String value) {
        try {
            URI uri = URI.create(value);
            if (!"https".equals(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null) {
                throw new IllegalArgumentException();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Stripe return URLs must be configured HTTPS URLs");
        }
    }

    @Bean
    StripeClient stripeClient() {
        return StripeClient.builder().setApiKey(secretKey).setMaxNetworkRetries(0)
                .setConnectTimeout(10000).setReadTimeout(30000).build();
    }
}
