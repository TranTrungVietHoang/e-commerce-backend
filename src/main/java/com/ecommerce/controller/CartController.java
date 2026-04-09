package com.ecommerce.controller;

import com.ecommerce.dto.request.cart.AddToCartRequest;
import com.ecommerce.dto.request.cart.UpdateCartItemRequest;
import com.ecommerce.dto.response.ApiResponse;
import com.ecommerce.dto.response.cart.CartResponse;
import com.ecommerce.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.access.prepost.PreAuthorize;
import com.ecommerce.service.UserService;

@RestController
@RequestMapping("/api/v1/cart")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getCart(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userService.getUserIdByUsername(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(cartService.getCart(userId)));
    }

    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartResponse>> addToCart(@AuthenticationPrincipal UserDetails userDetails, @Valid @RequestBody AddToCartRequest request) {
        Long userId = userService.getUserIdByUsername(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(cartService.addToCart(userId, request)));
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<CartResponse>> updateCartItem(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        Long userId = userService.getUserIdByUsername(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(cartService.updateCartItem(userId, itemId, request)));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<CartResponse>> removeCartItem(@AuthenticationPrincipal UserDetails userDetails, @PathVariable Long itemId) {
        Long userId = userService.getUserIdByUsername(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(cartService.removeCartItem(userId, itemId)));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<CartResponse>> clearCart(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userService.getUserIdByUsername(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(cartService.clearCart(userId)));
    }
}
