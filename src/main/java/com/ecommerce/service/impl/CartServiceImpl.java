package com.ecommerce.service.impl;

import com.ecommerce.dto.request.cart.AddToCartRequest;
import com.ecommerce.dto.request.cart.UpdateCartItemRequest;
import com.ecommerce.dto.response.cart.CartItemResponse;
import com.ecommerce.dto.response.cart.CartResponse;
import com.ecommerce.entity.Cart;
import com.ecommerce.entity.CartItem;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.ProductImage;
import com.ecommerce.entity.ProductVariant;
import com.ecommerce.entity.User;
import com.ecommerce.exception.BusinessException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.repository.CartItemRepository;
import com.ecommerce.repository.CartRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.ProductVariantRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.repository.FlashSaleProductRepository;
import com.ecommerce.entity.FlashSaleProduct;
import com.ecommerce.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final FlashSaleProductRepository flashSaleProductRepository;

    @Override
    @Transactional(readOnly = true)
    public CartResponse getCart(Long userId) {
        return toCartResponse(getOrCreateCart(userId));
    }

    @Override
    @Transactional
    public CartResponse addToCart(Long userId, AddToCartRequest request) {
        Cart cart = getOrCreateCart(userId);
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", request.getProductId()));

        validateProductCanBePurchased(product);
        ProductVariant variant = resolveVariant(product, request.getVariantId());
        int availableStock = getAvailableStock(product, variant);

        Optional<CartItem> existingItem = request.getVariantId() != null
                ? cartItemRepository.findByCartIdAndProductIdAndVariantId(cart.getId(), product.getId(), request.getVariantId())
                : cartItemRepository.findByCartIdAndProductIdAndVariantIsNull(cart.getId(), product.getId());

        int newQuantity = request.getQuantity() + existingItem.map(CartItem::getQuantity).orElse(0);
        validateStock(newQuantity, availableStock);

        CartItem item = existingItem.orElseGet(CartItem::new);
        boolean isNewItem = item.getId() == null;
        item.setCart(cart);
        item.setProduct(product);
        item.setVariant(variant);
        item.setQuantity(newQuantity);
        item.setUnitPrice(resolveCurrentUnitPrice(product, variant));
        item = cartItemRepository.save(item);

        if (isNewItem) {
            cart.getItems().add(item);
        }

        return toCartResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse updateCartItem(Long userId, Long itemId, UpdateCartItemRequest request) {
        Cart cart = getOrCreateCart(userId);
        CartItem item = cart.getItems().stream()
                .filter(cartItem -> cartItem.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("CartItem", itemId));

        validateProductCanBePurchased(item.getProduct());
        validateStock(request.getQuantity(), getAvailableStock(item.getProduct(), item.getVariant()));
        item.setQuantity(request.getQuantity());
        item.setUnitPrice(resolveCurrentUnitPrice(item.getProduct(), item.getVariant()));
        cartItemRepository.save(item);
        return toCartResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse removeCartItem(Long userId, Long itemId) {
        Cart cart = getOrCreateCart(userId);
        CartItem item = cart.getItems().stream()
                .filter(cartItem -> cartItem.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("CartItem", itemId));
        cart.getItems().removeIf(cartItem -> cartItem.getId().equals(itemId));
        cartItemRepository.delete(item);
        return toCartResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse clearCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        cart.getItems().clear();
        cartRepository.save(cart);
        return toCartResponse(cart);
    }

    private Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId).orElseGet(() -> {
            User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", userId));
            Cart cart = new Cart();
            cart.setUser(user);
            return cartRepository.save(cart);
        });
    }

    private ProductVariant resolveVariant(Product product, Long variantId) {
        if (variantId == null) {
            return null;
        }
        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("ProductVariant", variantId));
        if (!variant.getProduct().getId().equals(product.getId())) {
            throw new BusinessException("Bien the khong thuoc san pham da chon");
        }
        return variant;
    }

    private void validateProductCanBePurchased(Product product) {
        if (!"ACTIVE".equalsIgnoreCase(product.getStatus())) {
            throw new BusinessException("San pham hien khong kha dung");
        }
    }

    private void validateStock(int quantity, int availableStock) {
        if (quantity <= 0) {
            throw new BusinessException("So luong phai lon hon 0");
        }
        if (quantity > availableStock) {
            throw new BusinessException("So luong yeu cau vuot qua ton kho hien co");
        }
    }

    private int getAvailableStock(Product product, ProductVariant variant) {
        return variant != null ? variant.getStock() : product.getStockQuantity();
    }

    private BigDecimal resolveCurrentUnitPrice(Product product, ProductVariant variant) {
        BigDecimal baseVariantPrice = variant != null ? variant.getPrice() : product.getBasePrice();
        
        Optional<FlashSaleProduct> fspOpt = flashSaleProductRepository.findActiveByProductId(product.getId());
        if (fspOpt.isPresent()) {
            return fspOpt.get().getFlashSalePrice().min(baseVariantPrice);
        }
        
        return baseVariantPrice;
    }

    private CartResponse toCartResponse(Cart cart) {
        CartResponse response = new CartResponse();
        response.setCartId(cart.getId());
        response.setUserId(cart.getUser().getId());

        var sortedItems = cart.getItems().stream()
                .sorted(Comparator.comparing(CartItem::getId))
                .collect(Collectors.toList());

        response.setItems(sortedItems.stream().map(item -> {
            validateProductCanBePurchased(item.getProduct());

            BigDecimal currentUnitPrice = resolveCurrentUnitPrice(item.getProduct(), item.getVariant());
            if (item.getUnitPrice() == null || item.getUnitPrice().compareTo(currentUnitPrice) != 0) {
                item.setUnitPrice(currentUnitPrice);
                cartItemRepository.save(item);
            }

            CartItemResponse itemResponse = new CartItemResponse();
            itemResponse.setId(item.getId());
            itemResponse.setShopId(item.getProduct().getShop().getId());
            itemResponse.setShopName(item.getProduct().getShop().getName());
            itemResponse.setProductId(item.getProduct().getId());
            itemResponse.setVariantId(item.getVariant() != null ? item.getVariant().getId() : null);
            itemResponse.setProductName(item.getProduct().getName());
            itemResponse.setVariantName(item.getVariant() != null ? item.getVariant().getAttributes() : null);
            itemResponse.setImageUrl(item.getProduct().getImages().stream()
                    .filter(image -> Boolean.TRUE.equals(image.getIsPrimary()))
                    .map(ProductImage::getImageUrl)
                    .findFirst()
                    .orElse(item.getProduct().getImages().stream().findFirst().map(ProductImage::getImageUrl).orElse(null)));
            itemResponse.setQuantity(item.getQuantity());
            itemResponse.setAvailableStock(getAvailableStock(item.getProduct(), item.getVariant()));
            itemResponse.setUnitPrice(currentUnitPrice);
            itemResponse.setLineTotal(currentUnitPrice.multiply(BigDecimal.valueOf(item.getQuantity())));
            itemResponse.setBasePrice(item.getVariant() != null ? item.getVariant().getPrice() : item.getProduct().getBasePrice());
            
            // Cập nhật thông tin Flash Sale từ bảng mới
            flashSaleProductRepository.findActiveByProductId(item.getProduct().getId()).ifPresent(fsp -> {
                itemResponse.setFlashSaleActive(true);
                itemResponse.setFlashSalePrice(fsp.getFlashSalePrice());
                itemResponse.setFlashSaleEndAt(fsp.getFlashSale().getEndTime());
            });

            if (itemResponse.getFlashSaleActive() == null) {
                itemResponse.setFlashSaleActive(false);
            }

            return itemResponse;
        }).collect(Collectors.toList()));

        response.setTotalItems(sortedItems.stream().mapToInt(CartItem::getQuantity).sum());
        response.setSubtotal(response.getItems().stream()
                .map(CartItemResponse::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        return response;
    }
}
