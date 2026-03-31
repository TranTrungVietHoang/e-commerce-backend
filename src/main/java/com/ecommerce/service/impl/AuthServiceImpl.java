package com.ecommerce.service.impl;

import com.ecommerce.entity.User;
import com.ecommerce.entity.Role;
import com.ecommerce.exception.AppException;
import com.ecommerce.exception.ErrorCode;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.repository.RoleRepository;
import com.ecommerce.service.AuthService;
import com.ecommerce.service.OtpService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;

    @Override
    @Transactional
    public Object register(Object request) {
        // Lưu ý: Sau này bạn nên thay Object bằng RegisterRequest DTO
        // 1. Kiểm tra email tồn tại chưa (logic giả định)
        // 2. Lưu User với status = "INACTIVE"
        // 3. Gọi otpService.generateAndSendOtp(email)
        return "Đăng ký thành công. Vui lòng kiểm tra email để nhận mã OTP.";
    }

    @Override
    public Object login(String email, String password) {
        // Logic: Tìm user -> Kiểm tra password -> Tạo JWT Token
        return null; 
    }

    @Override
    public Object refreshToken(String refreshToken) {
        return null;
    }

    @Override
    public void logout(Long userId) {
        // Logic: Xóa Refresh Token trong DB
    }

    @Override
    @Transactional
    public void verifyEmail(String token) {
        // Logic: Dùng OtpService để xác thực mã token/otp
    }

    @Override
    public void forgotPassword(String email) {
        // Gọi trực tiếp hàm bạn đã viết trong OtpService
        otpService.generateAndSendOtp(email);
    }

    @Override
    @Transactional
    public void resetPassword(String token, String newPassword) {
        // Bạn có thể chỉnh lại OtpService để nhận token và pass mới
    }
}