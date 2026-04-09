package com.ecommerce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@org.springframework.scheduling.annotation.EnableScheduling
public class ECommerceApplication {
	public static void main(String[] args) {
		SpringApplication.run(ECommerceApplication.class, args);
	}
}
