package com.ecommerce.service;

import com.ecommerce.dto.response.WishlistResponse;
import com.ecommerce.entity.FlashSaleProduct;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.User;
import com.ecommerce.entity.Wishlist;
import com.ecommerce.exception.AppException;
import com.ecommerce.exception.ErrorCode;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.repository.WishlistRepository;
import com.ecommerce.repository.FlashSaleProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final FlashSaleProductRepository flashSaleProductRepository;

    @Transactional
    public WishlistResponse addToWishlist(Long userId, Long productId) {
        if (wishlistRepository.existsByUserIdAndProductId(userId, productId)) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Sản phẩm đã có trong danh sách yêu thích");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Sản phẩm không tồn tại"));

        Wishlist wishlist = Wishlist.builder()
                .user(user)
                .product(product)
                .build();

        wishlist = wishlistRepository.save(wishlist);
        return mapToResponse(wishlist);
    }

    @Transactional
    public void removeFromWishlist(Long userId, Long productId) {
        Wishlist wishlist = wishlistRepository.findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy sản phẩm trong danh sách yêu thích"));
        
        wishlistRepository.delete(wishlist);
    }

    @Transactional(readOnly = true)
    public Page<WishlistResponse> getUserWishlist(Long userId, Pageable pageable) {
        return wishlistRepository.findByUserId(userId, pageable)
                .map(this::mapToResponse);
    }

    private WishlistResponse mapToResponse(Wishlist wishlist) {
        Product product = wishlist.getProduct();
        String imageUrl = null;
        if (product.getImages() != null && !product.getImages().isEmpty()) {
            imageUrl = product.getImages().get(0).getImageUrl();
        }

        return WishlistResponse.builder()
                .id(wishlist.getId())
                .productId(product.getId())
                .productName(product.getName())
                .productPrice(resolveProductPrice(product))
                .productImageUrl(imageUrl)
                .build();
    }

    private java.math.BigDecimal resolveProductPrice(Product product) {
        List<FlashSaleProduct> activeFlashSales = flashSaleProductRepository.findActiveByProductId(product.getId());
        if (!activeFlashSales.isEmpty()) {
            return activeFlashSales.get(0).getFlashSalePrice();
        }
        return product.getBasePrice();
    }
}
