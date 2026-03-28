package com.ecommerce.service;

import com.ecommerce.dto.request.cart.AddToCartRequest;
import com.ecommerce.dto.request.cart.UpdateCartItemRequest;
import com.ecommerce.dto.response.cart.CartResponse;

public interface CartService {
    CartResponse getCart(Long userId);
    CartResponse addToCart(Long userId, AddToCartRequest request);
    CartResponse updateCartItem(Long userId, Long itemId, UpdateCartItemRequest request);
    CartResponse removeCartItem(Long userId, Long itemId);
    CartResponse clearCart(Long userId);
}
