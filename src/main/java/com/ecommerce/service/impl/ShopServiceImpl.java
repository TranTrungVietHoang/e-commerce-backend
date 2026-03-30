package com.ecommerce.service.impl;

import com.ecommerce.entity.Shop;
import com.ecommerce.entity.User;
import com.ecommerce.enums.ShopStatus;
import com.ecommerce.exception.AppException;
import com.ecommerce.exception.ErrorCode;
import com.ecommerce.repository.ShopRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.service.ShopService;
import com.ecommerce.dto.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ShopServiceImpl implements ShopService {

    private final ShopRepository shopRepository;
    private final UserRepository userRepository;

    @Override
    public Shop getShopInfo(Long shopId) {
        return shopRepository.findById(shopId)
                .orElseThrow(() -> new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION));
    }

    @Override
    @Transactional
    public Shop createShop(Long sellerId, Object request) {
        // 1. Kiểm tra User có tồn tại không
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // 2. Kiểm tra User đã có shop chưa (OneToOne)
        if (shopRepository.existsBySellerId(sellerId)) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION); // Nên là SHOP_ALREADY_EXISTS
        }

        Shop shop = (Shop) request;
        shop.setSeller(seller);
        shop.setStatus(ShopStatus.PENDING); // Luôn để PENDING khi mới đăng ký

        return shopRepository.save(shop);
    }

    @Override
    @Transactional
    public Shop updateShopInfo(Long shopId, Object request) {
        Shop existing = getShopInfo(shopId);
        Shop updateData = (Shop) request;

        existing.setName(updateData.getName());
        existing.setDescription(updateData.getDescription());
        existing.setLogoUrl(updateData.getLogoUrl());
        existing.setBannerUrl(updateData.getBannerUrl());

        return shopRepository.save(existing);
    }

    @Override
    public PageResponse<Shop> getShops(Pageable pageable) {
        Page<Shop> shopPage = shopRepository.findAll(pageable);

        // Sử dụng hàm static xịn bạn đã viết trong PageResponse
        return PageResponse.fromPage(shopPage);
    }

    // Thêm hàm cho Admin duyệt shop (Bạn có thể thêm vào interface sau)
    @Transactional
    public void approveShop(Long shopId, ShopStatus status) {
        Shop shop = getShopInfo(shopId);
        shop.setStatus(status);
        shopRepository.save(shop);
    }
}