package com.ecommerce.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Cấu hình API Sepay Payment Gateway
 */
@Component
@ConfigurationProperties(prefix = "sepay")
@Data
public class SepayConfig {
    private String apiId;
    private String apiKey;
    private String apiSecret;
    private String apiUrl;
    private String webhookSecret;
}
