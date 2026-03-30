package com.ecommerce.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
public class HelloController {

    @GetMapping("/")
    public Map<String, Object> welcome() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "E-Commerce Backend API");
        response.put("version", "1.0.0");
        response.put("timestamp", LocalDateTime.now());
        response.put("apiDocs", "/swagger-ui.html");
        return response;
    }

    @GetMapping("/api/test/hello")
    public Map<String, String> hello() {
        return Map.of("message", "Backend Spring Boot đã sẵn sàng! 🚀");
    }
}