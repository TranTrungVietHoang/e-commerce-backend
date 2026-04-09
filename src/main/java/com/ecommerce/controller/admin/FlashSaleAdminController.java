package com.ecommerce.controller.admin;

import com.ecommerce.dto.response.ApiResponse;
import com.ecommerce.entity.FlashSale;
import com.ecommerce.service.FlashSaleService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/flash-sales")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class FlashSaleAdminController {

    private final FlashSaleService flashSaleService;

    @PostMapping
    public ResponseEntity<ApiResponse<FlashSale>> create(@RequestBody FlashSaleRequest request) {
        return ResponseEntity.ok(ApiResponse.success(flashSaleService.createFlashSale(request.getName(), request.getStartTime(), request.getEndTime())));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<FlashSale>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(flashSaleService.getAllFlashSales()));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> updateStatus(@PathVariable Long id, @RequestParam String status) {
        flashSaleService.updateStatus(id, status);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        flashSaleService.deleteFlashSale(id);
        return ResponseEntity.noContent().build();
    }

    @Data
    public static class FlashSaleRequest {
        private String name;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
    }
}
