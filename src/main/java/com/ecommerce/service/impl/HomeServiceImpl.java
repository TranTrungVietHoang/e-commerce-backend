package com.ecommerce.service.impl;

import com.ecommerce.dto.response.BannerResponse;
import com.ecommerce.dto.response.HomeResponse;
import com.ecommerce.dto.response.product.ProductResponse;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.ProductImage;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.FlashSaleProductRepository;
import com.ecommerce.service.HomeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HomeServiceImpl implements HomeService {

    private final ProductRepository productRepository;
    private final FlashSaleProductRepository flashSaleProductRepository;

    @Override
    @Transactional(readOnly = true)
    public HomeResponse getHomeData() {
        HomeResponse response = new HomeResponse();

        // Banner tĩnh – có thể chuyển sang DB sau
        response.setBanners(List.of(
            new BannerResponse("Khuyến mãi mùa hè", "Giảm đến 50% hàng nghìn sản phẩm", "", "/search"),
            new BannerResponse("Hàng mới về mỗi ngày", "Khám phá xu hướng mới nhất", "", "/search?sort=newest"),
            new BannerResponse("Flash Sale 12H", "Săn deal cực sốc chỉ hôm nay", "", "/search")
        ));

        // 8 sản phẩm mới nhất (chỉ lấy đã duyệt)
        List<Product> newest = productRepository.findActiveAndApprovedOrderByCreatedAtDesc("ACTIVE", PageRequest.of(0, 8));
        response.setNewestProducts(newest.stream().map(this::mapToResponse).collect(Collectors.toList()));

        // 8 sản phẩm bán chạy nhất (chỉ lấy đã duyệt)
        List<Product> bestsellers = productRepository.findActiveAndApprovedOrderBySoldCountDesc("ACTIVE", PageRequest.of(0, 8));
        response.setBestSellingProducts(bestsellers.stream().map(this::mapToResponse).collect(Collectors.toList()));

        return response;
    }

    private ProductResponse mapToResponse(Product product) {
        ProductResponse res = new ProductResponse();
        res.setId(product.getId());
        res.setName(product.getName());
        res.setBasePrice(product.getBasePrice());
        res.setRating(product.getRating());
        res.setSoldCount(product.getSoldCount());
        res.setStatus(product.getStatus());
        res.setModerationStatus(product.getModerationStatus());
        res.setCreatedAt(product.getCreatedAt());
        res.setStockQuantity(product.getStockQuantity());
        res.setCategoryName(product.getCategory() != null ? product.getCategory().getName() : null);
        res.setPrimaryImageUrl(resolvePrimaryImage(product));

        // Tìm thông tin Flash Sale
        flashSaleProductRepository.findActiveByProductId(product.getId()).ifPresent(fsp -> {
            res.setFlashSaleActive(true);
            res.setFlashSalePrice(fsp.getFlashSalePrice());
            res.setFlashSaleStartAt(fsp.getFlashSale().getStartTime());
            res.setFlashSaleEndAt(fsp.getFlashSale().getEndTime());
            res.setEffectivePrice(fsp.getFlashSalePrice());
        });

        if (res.getEffectivePrice() == null) {
            res.setEffectivePrice(product.getBasePrice());
            res.setFlashSaleActive(false);
        }

        return res;
    }

    private String resolvePrimaryImage(Product product) {
        return product.getImages().stream()
                .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                .map(ProductImage::getImageUrl)
                .findFirst()
                .orElse(product.getImages().isEmpty() ? null : product.getImages().get(0).getImageUrl());
    }
}
