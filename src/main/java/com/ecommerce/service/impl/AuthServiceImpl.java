package com.ecommerce.service.impl;

import com.ecommerce.dto.request.auth.LoginRequest;
import com.ecommerce.dto.request.auth.RefreshTokenRequest;
import com.ecommerce.dto.request.auth.RegisterRequest;
import com.ecommerce.dto.response.auth.AuthResponse;
import com.ecommerce.entity.User;
import com.ecommerce.entity.Role;
import com.ecommerce.exception.AppException;
import com.ecommerce.exception.ErrorCode;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.repository.RoleRepository;
import com.ecommerce.security.JwtUtil;
import com.ecommerce.service.AuthService;
import com.ecommerce.service.OtpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final JwtUtil jwtUtil; // Added to generate real tokens

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.USER_ALREADY_EXISTS, "Email đã được sử dụng");
        }

        if (request.getPhone() != null && userRepository.existsByPhone(request.getPhone())) {
            throw new AppException(ErrorCode.USER_ALREADY_EXISTS, "Số điện thoại đã được sử dụng bởi tài khoản khác");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .status("ACTIVE") // Set ACTIVE immediately so we don't need OTP verification to test
                .build();
                
        // Find default role (usually ID=1 for CUSTOMER) or just let it be empty until assigned
        Optional<Role> customerRole = roleRepository.findByName("ROLE_CUSTOMER");
        customerRole.ifPresent(r -> user.getRoles().add(r));

        userRepository.save(user);

        // Generate token immediately so user doesn't have to verify OTP in testing
        String accessToken = jwtUtil.generateAccessToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .message("Đăng ký thành công (Bypassed OTP for testing).")
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        log.info("Đang xử lý đăng nhập cho email: {}", request.getEmail());
        
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "Sai email hoặc mật khẩu"));
                
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Sai email hoặc mật khẩu");
        }

        String accessToken = jwtUtil.generateAccessToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());

        // DEBUG: In token ra console để người dùng lấy khi bị Edge chặn storage
        System.out.println("\n--- DEBUG TOKEN FOR USER: " + user.getEmail() + " ---");
        System.out.println("ACCESS_TOKEN: " + accessToken);
        System.out.println("----------------------------------------------\n");

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .roles(user.getRoles().stream().map(Role::getName).toList())
                .message("Đăng nhập thành công")
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
    }

    @Override
    @Transactional
    public void verifyEmail(String token) {
    }

    @Override
    public void forgotPassword(String email) {
    }

    @Override
    @Transactional
    public void resetPassword(String token, String newPassword) {
    }
}