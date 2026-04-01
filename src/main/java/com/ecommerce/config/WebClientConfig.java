package com.ecommerce.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Cấu hình RestTemplate cho gọi API bên ngoài (Sepay, VNPAY, ...)
 */
@Configuration
public class WebClientConfig {

  /**
   * Tạo RestTemplate bean với timeout mặc định
   */
  @Bean
  public RestTemplate restTemplate(RestTemplateBuilder builder) {
    return builder
        .setConnectTimeout(java.time.Duration.ofSeconds(10))
        .setReadTimeout(java.time.Duration.ofSeconds(10))
        .build();
  }
}
