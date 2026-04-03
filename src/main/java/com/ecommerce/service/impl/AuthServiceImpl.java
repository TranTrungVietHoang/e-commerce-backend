package com.ecommerce.service.impl;

import com.ecommerce.dto.request.auth.LoginRequest;
import com.ecommerce.dto.request.auth.RefreshTokenRequest;
import com.ecommerce.dto.request.auth.RegisterRequest;
import com.ecommerce.dto.response.auth.AuthResponse;
import com.ecommerce.entity.User;
import com.ecommerce.entity.Role;
import com.ecommerce.entity.Shop;
import com.ecommerce.exception.AppException;
import com.ecommerce.exception.ErrorCode;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.repository.RoleRepository;
import com.ecommerce.repository.ShopRepository;
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
    private final ShopRepository shopRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final JwtUtil jwtUtil;


    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.USER_ALREADY_EXISTS, "Email đã được sử dụng");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .status("ACTIVE")
                .build();
                
        Optional<Role> customerRole = roleRepository.findByName("ROLE_CUSTOMER");
        customerRole.ifPresent(r -> user.getRoles().add(r));

        userRepository.save(user);

        String accessToken = jwtUtil.generateAccessToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .message("Đăng ký thành công")
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

        // Get role names
        java.util.List<String> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .collect(java.util.stream.Collectors.toList());

        AuthResponse.AuthResponseBuilder builder = AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .roles(roleNames)
                .message("Đăng nhập thành công");

        // If user is a seller, fetch their shop ID
        if (roleNames.contains("ROLE_SELLER")) {
            Optional<Shop> shop = shopRepository.findBySellerId(user.getId());
            if (shop.isPresent()) {
                builder.shopId(shop.get().getId());
            }
        }

        return builder.build();
    }

    @Override
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        log.info("Đang làm mới token");
        
        if (!jwtUtil.validateToken(request.getRefreshToken())) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Refresh Token không hợp lệ hoặc đã hết hạn");
        }

        String email = jwtUtil.extractUsername(request.getRefreshToken());
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "Người dùng không tồn tại"));

        String newAccessToken = jwtUtil.generateAccessToken(user);

        // Get role names
        java.util.List<String> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .collect(java.util.stream.Collectors.toList());

        AuthResponse.AuthResponseBuilder builder = AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(request.getRefreshToken())
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .roles(roleNames)
                .message("Làm mới token thành công");

        // If user is a seller, fetch their shop ID
        if (roleNames.contains("ROLE_SELLER")) {
            Optional<Shop> shop = shopRepository.findBySellerId(user.getId());
            if (shop.isPresent()) {
                builder.shopId(shop.get().getId());
            }
        }

        return builder.build();
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