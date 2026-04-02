package com.ecommerce.service;

import com.ecommerce.dto.request.user.ChangePasswordRequest;
import com.ecommerce.dto.request.user.UpdateProfileRequest;
import com.ecommerce.dto.response.user.UserResponse;
import com.ecommerce.entity.User;
import com.ecommerce.exception.AppException;
import com.ecommerce.exception.ErrorCode;
import com.ecommerce.repository.ShopRepository;
import com.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ShopRepository shopRepository;
    private final PasswordEncoder passwordEncoder;
    private final CloudinaryService cloudinaryService;

    public UserResponse getProfile(String email) {
        User user = getUserByEmail(email);
        return mapToResponse(user);
    }

    @Transactional
    public UserResponse updateProfile(String email, UpdateProfileRequest request) {
        User user = getUserByEmail(email);

        if (request.getPhone() != null && !request.getPhone().isBlank()
                && !request.getPhone().equals(user.getPhone())
                && userRepository.existsByPhone(request.getPhone())) {
            throw new AppException(ErrorCode.PHONE_EXISTS);
        }

        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());

        // Chỉ cập nhật avatarUrl nếu có truyền lên
        if (request.getAvatarUrl() != null && !request.getAvatarUrl().isBlank()) {
            user.setAvatarUrl(request.getAvatarUrl());
        }

        return mapToResponse(userRepository.save(user));
    }

    /**
     * Upload ảnh đại diện lên Cloudinary, lưu URL vào DB, trả về URL.
     */
    @Transactional
    public String uploadAvatar(String email, MultipartFile file) {
        User user = getUserByEmail(email);
        String url = cloudinaryService.uploadImage(file, "avatars");
        user.setAvatarUrl(url);
        userRepository.save(user);
        return url;
    }

    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = getUserByEmail(email);

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new AppException(ErrorCode.WRONG_CURRENT_PASSWORD);
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    /**
     * Lấy user ID từ email/username
     */
    public Long getUserIdByUsername(String email) {
        return getUserByEmail(email).getId();
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private UserResponse mapToResponse(User user) {
        List<String> roles = user.getRoles().stream()
                .map(r -> r.getName())
                .collect(Collectors.toList());
        
        // Lấy shopId nếu user là seller
        Long shopId = null;
        if (roles.contains("ROLE_SELLER")) {
            shopId = shopRepository.findBySellerId(user.getId())
                    .map(shop -> shop.getId())
                    .orElse(null);
        }
        
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .status(user.getStatus())
                .avatarUrl(user.getAvatarUrl())
                .roles(roles)
                .shopId(shopId)
                .build();
    }
}
