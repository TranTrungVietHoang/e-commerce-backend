package com.ecommerce.service;

import com.ecommerce.dto.request.shop.ShopRegistrationRequest;
import com.ecommerce.dto.request.shop.ShopUpdateRequest;
import com.ecommerce.dto.response.PageResponse;
import com.ecommerce.dto.response.shop.ShopResponse;
import com.ecommerce.enums.ShopStatus;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface ShopService {
    ShopResponse createShop(Long sellerId, ShopRegistrationRequest request);

    ShopResponse updateShopInfo(Long sellerId, ShopUpdateRequest request);

    ShopResponse getShopBySellerId(Long sellerId);

    ShopResponse getShopById(Long id);

    List<ShopResponse> getAllShops();
    
    List<ShopResponse> getPendingShops();

    PageResponse<ShopResponse> getShops(Pageable pageable);

    void approveShop(Long shopId, ShopStatus status);
}