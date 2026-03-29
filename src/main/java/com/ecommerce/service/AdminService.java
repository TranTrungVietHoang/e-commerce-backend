package com.ecommerce.service;

import com.ecommerce.dto.response.user.UserResponse;
import com.ecommerce.entity.User;
import com.ecommerce.exception.AppException;
import com.ecommerce.exception.ErrorCode;
import com.ecommerce.repository.RefreshTokenRepository;
import com.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    /** Tìm kiếm + phân trang danh sách users */
    public Page<UserResponse> getUsers(String keyword, String status, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        String kw = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        String st = (status != null && !status.isBlank()) ? status.trim() : null;
        return userRepository.searchUsers(kw, st, pageable).map(this::mapToResponse);
    }

    /** Chi tiết 1 user */
    public UserResponse getUserById(Long id) {
        return mapToResponse(findUser(id));
    }

    /** Khóa hoặc Mở khóa tài khoản */
    @Transactional
    public UserResponse updateStatus(Long id, String newStatus) {
        User user = findUser(id);

        // Không cho phép khóa tài khoản Admin
        boolean isAdmin = user.getRoles().stream()
                .anyMatch(r -> "ROLE_ADMIN".equals(r.getName()));
        if (isAdmin) {
            throw new AppException(ErrorCode.CANNOT_LOCK_ADMIN);
        }

        user.setStatus(newStatus);
        userRepository.save(user);

        // Nếu khóa → ép đăng xuất ngay lập tức
        if ("LOCKED".equals(newStatus)) {
            refreshTokenRepository.deleteByUserId(user.getId());
        }

        return mapToResponse(user);
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private UserResponse mapToResponse(User user) {
        List<String> roles = user.getRoles().stream()
                .map(r -> r.getName())
                .toList();
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .status(user.getStatus())
                .roles(roles)
                .build();
    }
}
