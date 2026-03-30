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

import java.util.List;

@Service
@RequiredArgsConstructor
public class ShopServiceImpl implements ShopService {

    private final ShopRepository shopRepository;
    private final UserRepository userRepository;

    @Override
    public Shop getShopInfo(Long shopId) {
        return shopRepository.findById(shopId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    @Override
    public Shop getShopById(Long id) {
        return getShopInfo(id);
    }

    @Override
    public List<Shop> getAllShops() {
        return shopRepository.findAll();
    }

    @Override
    @Transactional
    public Shop createShop(Long sellerId, Shop shop) {
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (shopRepository.existsBySellerId(sellerId)) {
            throw new AppException(ErrorCode.USER_ALREADY_EXISTS, "Người dùng đã có cửa hàng");
        }

        shop.setSeller(seller);
        shop.setStatus(ShopStatus.PENDING);
        return shopRepository.save(shop);
    }

    @Override
    @Transactional
    public Shop updateShopInfo(Long shopId, Shop updateData) {
        Shop existing = getShopInfo(shopId);

        existing.setName(updateData.getName());
        existing.setDescription(updateData.getDescription());
        existing.setLogoUrl(updateData.getLogoUrl());
        existing.setBannerUrl(updateData.getBannerUrl());

        return shopRepository.save(existing);
    }

    @Override
    public PageResponse<Shop> getShops(Pageable pageable) {
        Page<Shop> shopPage = shopRepository.findAll(pageable);
        
        // Nếu dòng dưới vẫn báo lỗi "undefined", hãy xem bước 3 bên dưới
        return PageResponse.<Shop>builder()
                .content(shopPage.getContent())
                .pageSize(shopPage.getSize())
                .pageNumber(shopPage.getNumber())
                .totalElements(shopPage.getTotalElements())
                .totalPages(shopPage.getTotalPages())
                .build();
    }

    @Override
    @Transactional
    public void approveShop(Long shopId, ShopStatus status) {
        Shop shop = getShopInfo(shopId);
        shop.setStatus(status);
        shopRepository.save(shop);
    }
}