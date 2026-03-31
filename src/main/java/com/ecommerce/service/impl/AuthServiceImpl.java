package com.ecommerce.service.impl;

import com.ecommerce.dto.request.auth.LoginRequest;
import com.ecommerce.dto.request.auth.RefreshTokenRequest;
import com.ecommerce.dto.request.auth.RegisterRequest;
import com.ecommerce.dto.response.auth.AuthResponse;
import com.ecommerce.entity.User;
import com.ecommerce.exception.AppException;
import com.ecommerce.exception.ErrorCode;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.repository.RoleRepository;
import com.ecommerce.service.AuthService;
import com.ecommerce.service.OtpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // 1. Kiểm tra email tồn tại chưa
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.USER_ALREADY_EXISTS, "Email đã được sử dụng");
        }

        // 2. Logic tạo User (giả định)
        // User user = User.builder()
        //         .email(request.getEmail())
        //         .password(passwordEncoder.encode(request.getPassword()))
        //         .status("INACTIVE")
        //         .build();
        // userRepository.save(user);

        // 3. Gửi OTP
        otpService.generateAndSendOtp(request.getEmail());

        return AuthResponse.builder()
                .message("Đăng ký thành công. Vui lòng kiểm tra email để nhận mã OTP.")
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        // Logic: Tìm user -> Kiểm tra password -> Tạo JWT Token
        // Đây là nơi bạn gọi JwtTokenProvider để tạo token
        log.info("Đang xử lý đăng nhập cho email: {}", request.getEmail());
        
        // Tạm thời trả về object trống để không lỗi code
        return AuthResponse.builder()
                .accessToken("dummy-access-token")
                .refreshToken("dummy-refresh-token")
                .build();
    }

    @Override
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        log.info("Đang làm mới token");
        return AuthResponse.builder()
                .accessToken("new-access-token")
                .build();
    }

    @Override
    @Transactional
    public void logout(String username) {
        log.info("User {} đang đăng xuất", username);
        // Logic: Vô hiệu hóa token trong DB hoặc Redis nếu có
    }

    @Override
    @Transactional
    public void verifyEmail(String token) {
        // Logic xác thực email
    }

    @Override
    public void forgotPassword(String email) {
        otpService.generateAndSendOtp(email);
    }

    @Override
    @Transactional
    public void resetPassword(String token, String newPassword) {
        // Logic đặt lại mật khẩu
    }
}