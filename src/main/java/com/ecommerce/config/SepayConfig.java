package com.ecommerce.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "sepay")
@Data
public class SepayConfig {
    private String apiUrl = "https://my.sepay.vn/api/v2";
    private String apiKey;
    private String accountNumber;
    private String accountName;
}
