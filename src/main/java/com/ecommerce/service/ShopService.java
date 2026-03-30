package com.ecommerce.service;

import com.ecommerce.entity.Shop;
import com.ecommerce.enums.ShopStatus;
import com.ecommerce.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface ShopService {
    Shop createShop(Long sellerId, Shop shop);

    Shop updateShopInfo(Long shopId, Shop shop);

    Shop getShopInfo(Long shopId);

    Shop getShopById(Long id);

    List<Shop> getAllShops();

    PageResponse<Shop> getShops(Pageable pageable);

    void approveShop(Long shopId, ShopStatus status);
}