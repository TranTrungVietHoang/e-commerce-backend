package com.ecommerce.controller;

import com.ecommerce.dto.response.ApiResponse;
import com.ecommerce.entity.FlashSale;
import com.ecommerce.service.FlashSaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/flash-sales")
@RequiredArgsConstructor
public class FlashSaleController {

    private final FlashSaleService flashSaleService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<FlashSale>>> getAll() {
        // Trả về tất cả các đợt Flash Sale bọc trong ApiResponse chuẩn
        return ResponseEntity.ok(ApiResponse.success(flashSaleService.getAllFlashSales()));
    }
}
