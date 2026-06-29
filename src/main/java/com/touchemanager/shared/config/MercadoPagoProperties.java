package com.touchemanager.shared.config;

import com.mercadopago.MercadoPagoConfig;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "mercadopago")
@Getter
@Setter
@Slf4j
public class MercadoPagoProperties {
    private boolean enabled = false;
    private String accessToken;
    private String frontendUrl = "http://localhost:4200";
    private String backendUrl = "http://localhost:8080";

    @PostConstruct
    public void init() {
        if (enabled) {
            if (accessToken == null || accessToken.isBlank()) {
                log.error("Mercado Pago is enabled but access-token is not configured!");
                throw new IllegalStateException("Mercado Pago is enabled but access-token is missing");
            }
            try {
                MercadoPagoConfig.setAccessToken(accessToken);
                log.info("Mercado Pago SDK initialized successfully.");
            } catch (Exception e) {
                log.error("Failed to initialize Mercado Pago SDK with access-token", e);
                throw new IllegalStateException("Failed to initialize Mercado Pago SDK", e);
            }
        } else {
            log.info("Mercado Pago integration is disabled. Local simulator will be used.");
        }
    }
}
