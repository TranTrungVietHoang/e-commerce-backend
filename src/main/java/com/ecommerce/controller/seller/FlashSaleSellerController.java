package com.ecommerce.controller.seller;

import com.ecommerce.entity.FlashSaleProduct;
import com.ecommerce.entity.Shop;
import com.ecommerce.entity.User;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.repository.ShopRepository;
import com.ecommerce.service.FlashSaleProductService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/seller/flash-sales")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SELLER')")
public class FlashSaleSellerController {

    private final FlashSaleProductService flashSaleProductService;
    private final UserRepository userRepository;
    private final ShopRepository shopRepository;

    @PostMapping("/register")
    public ResponseEntity<FlashSaleProduct> register(
            Authentication authentication,
            @RequestBody RegisterFlashSaleRequest request) {
        User seller = userRepository.findFirstByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Seller not found"));

        Shop shop = shopRepository.findFirstBySellerId(seller.getId())
                .orElseThrow(() -> new RuntimeException("Shop not found for this seller"));

        return ResponseEntity.ok(flashSaleProductService.registerProduct(
                request.getFlashSaleId(),
                request.getProductId(),
                request.getFlashSalePrice(),
                request.getSlots(),
                shop.getId()));
    }

    @GetMapping("/my-products")
    public ResponseEntity<List<FlashSaleProduct>> getMyProducts(Authentication authentication) {
        User seller = userRepository.findFirstByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Seller not found"));

        Shop shop = shopRepository.findFirstBySellerId(seller.getId())
                .orElseThrow(() -> new RuntimeException("Shop not found for this seller"));

        return ResponseEntity.ok(flashSaleProductService.getProductsByShop(shop.getId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> unregister(Authentication authentication, @PathVariable Long id) {
        User seller = userRepository.findFirstByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Seller not found"));

        Shop shop = shopRepository.findFirstBySellerId(seller.getId())
                .orElseThrow(() -> new RuntimeException("Shop not found for this seller"));

        flashSaleProductService.unregisterProduct(id, shop.getId());
        return ResponseEntity.noContent().build();
    }

    @Data
    public static class RegisterFlashSaleRequest {
        private Long flashSaleId;
        private Long productId;
        private BigDecimal flashSalePrice;
        private Integer slots;
    }
}
