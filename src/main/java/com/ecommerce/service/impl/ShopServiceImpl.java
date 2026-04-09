package com.ecommerce.service.impl;
import com.ecommerce.entity.Role;
import com.ecommerce.repository.RoleRepository;

import com.ecommerce.dto.request.shop.ShopRegistrationRequest;
import com.ecommerce.dto.request.shop.ShopUpdateRequest;
import com.ecommerce.dto.response.PageResponse;
import com.ecommerce.dto.response.shop.ShopResponse;
import com.ecommerce.entity.Shop;
import com.ecommerce.entity.User;
import com.ecommerce.enums.ShopStatus;
import com.ecommerce.exception.AppException;
import com.ecommerce.exception.ErrorCode;
import com.ecommerce.repository.ShopRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.service.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShopServiceImpl implements ShopService {

    private final ShopRepository shopRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Override
    public ShopResponse getShopBySellerId(Long sellerId) {
        Shop shop = shopRepository.findFirstBySellerId(sellerId)
                .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));
        return mapToResponse(shop);
    }

    @Override
    public ShopResponse getShopById(Long id) {
        Shop shop = shopRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));
        return mapToResponse(shop);
    }

    @Override
    public List<ShopResponse> getAllShops() {
        return shopRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ShopResponse> getPendingShops() {
        return shopRepository.findByStatus(ShopStatus.PENDING).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ShopResponse createShop(Long sellerId, ShopRegistrationRequest request) {
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (shopRepository.existsBySellerId(sellerId)) {
            throw new AppException(ErrorCode.SHOP_ALREADY_EXISTS);
        }

        if (shopRepository.existsByName(request.getName())) {
            throw new AppException(ErrorCode.SHOP_NAME_EXISTS);
        }

        Shop shop = Shop.builder()
                .seller(seller)
                .name(request.getName())
                .description(request.getDescription())
                .logoUrl(request.getLogoUrl())
                .bannerUrl(request.getBannerUrl())
                .status(ShopStatus.PENDING)
                .build();

        shop = shopRepository.save(shop);
        // Không nâng cấp role ngay - phải chờ Admin duyệt
        return mapToResponse(shop);
    }

    @Override
    @Transactional
    public ShopResponse updateShopInfo(Long sellerId, ShopUpdateRequest request) {
        Shop existing = shopRepository.findFirstBySellerId(sellerId)
                .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));

        if (request.getName() != null && !request.getName().equals(existing.getName())) {
            if (shopRepository.existsByName(request.getName())) {
                throw new AppException(ErrorCode.SHOP_NAME_EXISTS);
            }
            existing.setName(request.getName());
        }

        if (request.getDescription() != null) {
            existing.setDescription(request.getDescription());
        }
        if (request.getLogoUrl() != null) {
            existing.setLogoUrl(request.getLogoUrl());
        }
        if (request.getBannerUrl() != null) {
            existing.setBannerUrl(request.getBannerUrl());
        }

        existing = shopRepository.save(existing);
        return mapToResponse(existing);
    }

    @Override
    public PageResponse<ShopResponse> getShops(Pageable pageable) {
        Page<Shop> shopPage = shopRepository.findAll(pageable);
        
        List<ShopResponse> responses = shopPage.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return PageResponse.<ShopResponse>builder()
                .content(responses)
                .pageSize(shopPage.getSize())
                .pageNumber(shopPage.getNumber())
                .totalElements(shopPage.getTotalElements())
                .totalPages(shopPage.getTotalPages())
                .build();
    }

    @Override
    @Transactional
    public void approveShop(Long shopId, ShopStatus status) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));
        shop.setStatus(status);
        shopRepository.save(shop);

        User seller = shop.getSeller();

        if (status == ShopStatus.APPROVED) {
            // Duyệt: Cấp quyền SELLER cho user
            Role sellerRole = roleRepository.findByName("ROLE_SELLER")
                    .orElseThrow(() -> new RuntimeException("Error: Role SELLER is not found."));
            seller.getRoles().add(sellerRole);
            userRepository.save(seller);
        } else if (status == ShopStatus.REJECTED) {
            // Từ chối: Thu hồi quyền SELLER nếu có
            Role sellerRole = roleRepository.findByName("ROLE_SELLER").orElse(null);
            if (sellerRole != null) {
                seller.getRoles().remove(sellerRole);
                userRepository.save(seller);
            }
        }
    }

    private ShopResponse mapToResponse(Shop shop) {
        ShopResponse response = new ShopResponse();
        response.setId(shop.getId());
        response.setSellerId(shop.getSeller().getId());
        response.setSellerName(shop.getSeller().getFullName());
        response.setName(shop.getName());
        response.setDescription(shop.getDescription());
        response.setLogoUrl(shop.getLogoUrl());
        response.setBannerUrl(shop.getBannerUrl());
        response.setStatus(shop.getStatus());
        response.setRating(shop.getRating());
        response.setCreatedAt(shop.getCreatedAt());
        response.setUpdatedAt(shop.getUpdatedAt());
        return response;
    }
}
