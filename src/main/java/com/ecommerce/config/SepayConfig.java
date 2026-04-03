package com.ecommerce.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Cấu hình API Sepay Payment Gateway
 */
@Configuration
@ConfigurationProperties(prefix = "sepay")
@Data
public class SepayConfig {
    private String apiId;
    private String apiKey;
    private String apiSecret;
    private String apiUrl = "https://my.sepay.vn/api/v2";
    private String webhookSecret;
    private String accountNumber;
    private String accountName;
}
