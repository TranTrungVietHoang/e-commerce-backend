package com.ecommerce.controller;

import com.ecommerce.service.AuthService;
import com.ecommerce.service.OtpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Các API liên quan đến xác thực và OTP")
public class AuthController {

    private final AuthService authService;
    private final OtpService otpService;

    @PostMapping("/register")
    @Operation(summary = "Đăng ký tài khoản mới")
    public ResponseEntity<Object> register(@RequestBody Object request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Yêu cầu gửi OTP quên mật khẩu")
    public ResponseEntity<String> forgotPassword(@RequestParam String email) {
        otpService.generateAndSendOtp(email);
        return ResponseEntity.ok("Mã OTP đã được gửi đến email của bạn.");
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Xác nhận OTP và đặt lại mật khẩu")
    public ResponseEntity<String> resetPassword(
            @RequestParam String email,
            @RequestParam String otp,
            @RequestParam String newPassword) {
        otpService.verifyAndResetPassword(email, otp, newPassword);
        return ResponseEntity.ok("Mật khẩu đã được thay đổi thành công.");
    }

    @PostMapping("/login")
    @Operation(summary = "Đăng nhập hệ thống")
    public ResponseEntity<Object> login(@RequestParam String email, @RequestParam String password) {
        return ResponseEntity.ok(authService.login(email, password));
    }
}