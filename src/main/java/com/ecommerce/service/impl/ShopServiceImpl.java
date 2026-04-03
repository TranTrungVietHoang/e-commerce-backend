package com.ecommerce.service.impl;

import com.ecommerce.dto.request.shop.ShopRegistrationRequest;
import com.ecommerce.dto.request.shop.ShopUpdateRequest;
import com.ecommerce.dto.response.PageResponse;
import com.ecommerce.dto.response.shop.ShopResponse;
import com.ecommerce.entity.Shop;
import com.ecommerce.entity.User;
import com.ecommerce.enums.ShopStatus;
import com.ecommerce.exception.AppException;
import com.ecommerce.exception.ErrorCode;
import com.ecommerce.entity.Role;
import com.ecommerce.repository.RoleRepository;
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
    @Transactional(readOnly = true)
    public ShopResponse getShopBySellerId(Long sellerId) {
        Shop shop = shopRepository.findBySellerId(sellerId)
                .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));
        return mapToResponse(shop);
    }

    @Override
    @Transactional(readOnly = true)
    public ShopResponse getShopById(Long id) {
        Shop shop = shopRepository.findByIdWithSeller(id)
                .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));
        return mapToResponse(shop);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShopResponse> getAllShops() {
        return shopRepository.findAllWithSeller().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShopResponse> getPendingShops() {
        return shopRepository.findByStatusWithSeller(ShopStatus.PENDING).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ShopResponse createShop(Long sellerId, ShopRegistrationRequest request) {
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // 1. Chặn Admin tạo Shop
        boolean isAdmin = seller.getRoles().stream()
                .anyMatch(role -> role.getName().equals("ROLE_ADMIN"));
        if (isAdmin) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Quản trị viên không được phép tạo gian hàng cá nhân");
        }

        // 2. Kiểm tra Shop đã tồn tại chưa
        java.util.Optional<Shop> existingShop = shopRepository.findBySellerId(sellerId);
        
        if (existingShop.isPresent()) {
            Shop shop = existingShop.get();
            // Nếu đã APPROVED hoặc đang PENDING thì không cho tạo thêm
            if (shop.getStatus() == ShopStatus.APPROVED || shop.getStatus() == ShopStatus.PENDING) {
                throw new AppException(ErrorCode.SHOP_ALREADY_EXISTS, "Bạn đã có gian hàng hoặc đang chờ duyệt");
            }
            // Nếu là REJECTED -> Cho phép cập nhật thông tin và gửi lại (PENDING)
            shop.setName(request.getName());
            shop.setDescription(request.getDescription());
            shop.setLogoUrl(request.getLogoUrl());
            shop.setBannerUrl(request.getBannerUrl());
            shop.setStatus(ShopStatus.PENDING);
            shop.setRejectionReason(null); // Xóa lý do cũ
            shop = shopRepository.save(shop);
            return mapToResponse(shop);
        }

        // 3. Kiểm tra tên shop mới (không tính shop hiện tại vì đã handled ở trên)
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
        return mapToResponse(shop);
    }

    @Override
    @Transactional
    public ShopResponse updateShopInfo(Long sellerId, ShopUpdateRequest request) {
        Shop existing = shopRepository.findBySellerId(sellerId)
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
    public void approveShop(Long shopId, ShopStatus status, String reason) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new AppException(ErrorCode.SHOP_NOT_FOUND));
        shop.setStatus(status);
        shop.setRejectionReason(reason);
        
        // Nếu duyệt, gán thêm ROLE_SELLER cho người đăng ký
        if (status == ShopStatus.APPROVED) {
            Role sellerRole = roleRepository.findByName("ROLE_SELLER")
                    .orElseThrow(() -> new RuntimeException("ROLE_SELLER không tồn tại"));
            User seller = shop.getSeller();
            if (!seller.getRoles().contains(sellerRole)) {
                seller.getRoles().add(sellerRole);
                userRepository.save(seller);
            }
        }
        
        shopRepository.save(shop);
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
        response.setRejectionReason(shop.getRejectionReason());
        response.setRating(shop.getRating());
        response.setCreatedAt(shop.getCreatedAt());
        response.setUpdatedAt(shop.getUpdatedAt());
        return response;
    }
}