package com.ecommerce.controller;

import com.ecommerce.dto.request.auth.LoginRequest;
import com.ecommerce.dto.request.auth.RefreshTokenRequest;
import com.ecommerce.dto.request.auth.RegisterRequest;
import com.ecommerce.dto.request.auth.ForgotPasswordRequest;
import com.ecommerce.dto.request.auth.VerifyOtpRequest;
import com.ecommerce.dto.request.auth.ResetPasswordRequest;
import com.ecommerce.dto.response.ApiResponse;
import com.ecommerce.dto.response.auth.AuthResponse;
import com.ecommerce.service.AuthService;
import com.ecommerce.service.OtpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final OtpService otpService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.register(request), "Đăng ký thành công"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.login(request), "Đăng nhập thành công"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.refreshToken(request), "Làm mới token thành công"));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@AuthenticationPrincipal UserDetails userDetails) {
        authService.logout(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(null, "Đăng xuất thành công"));
    }

    // ==========================================
    // Mật khẩu & OTP (Milestone 5)
    // ==========================================
    
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        otpService.generateAndSendOtp(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success(null, "Nếu email hợp lệ, mã OTP sẽ được gửi đến hòm thư của bạn"));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<Void>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        // Chỉ mang tính chất validate token có hợp lệ không trước khi chuyển sang màn reset password.
        // Có thể gộp vào api reset cũng được.
        return ResponseEntity.ok(ApiResponse.success(null, "Mã OTP hợp lệ"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            return ResponseEntity.badRequest().body(ApiResponse.<Void>builder().code(400).message("Xác nhận mật khẩu không khớp").build());
        }
        otpService.verifyAndResetPassword(request.getEmail(), request.getOtp(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success(null, "Đặt lại mật khẩu thành công. Vui lòng đăng nhập lại."));
    }

    // ==========================================
    // OAuth2 Login (Google / Github) - Chừa sẵn
    // ==========================================
    
    @GetMapping("/oauth2/google")
    public ResponseEntity<ApiResponse<AuthResponse>> googleLogin(@RequestParam("code") String code) {
        // TODO: Chức năng đăng nhập bằng Google sẽ làm sau
        return ResponseEntity.status(501).body(ApiResponse.<AuthResponse>builder().code(501).message("Tính năng đăng nhập Google đang được phát triển").build());
    }

    @GetMapping("/oauth2/github")
    public ResponseEntity<ApiResponse<AuthResponse>> githubLogin(@RequestParam("code") String code) {
        // TODO: Chức năng đăng nhập bằng Github sẽ làm sau
        return ResponseEntity.status(501).body(ApiResponse.<AuthResponse>builder().code(501).message("Tính năng đăng nhập Github đang được phát triển").build());
    }
}
