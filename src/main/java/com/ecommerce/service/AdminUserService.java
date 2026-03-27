package com.ecommerce.service;

import com.ecommerce.dto.response.PageResponse;
import com.ecommerce.dto.response.user.UserResponse;
import com.ecommerce.entity.User;
import com.ecommerce.exception.AppException;
import com.ecommerce.exception.ErrorCode;
import com.ecommerce.repository.RefreshTokenRepository;
import com.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public PageResponse<UserResponse> getAllUsers(String keyword, String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<User> usersPage = userRepository.searchUsers(keyword, status, pageable);

        List<UserResponse> userResponses = usersPage.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return PageResponse.<UserResponse>builder()
                .pageNumber(usersPage.getNumber())
                .pageSize(usersPage.getSize())
                .totalPages(usersPage.getTotalPages())
                .totalElements(usersPage.getTotalElements())
                .items(userResponses)
                .build();
    }

    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        return mapToResponse(user);
    }

    @Transactional
    public void updateUserStatus(Long id, String newStatus) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (user.getRoles().stream().anyMatch(role -> role.getName().equals("ROLE_ADMIN"))) {
            throw new AppException(ErrorCode.ACCESS_DENIED); // Cấm khóa ADMIN khác
        }

        user.setStatus(newStatus.toUpperCase());
        userRepository.save(user);

        // Kích xuất người dùng ngay lập tức nếu bị khóa
        if ("LOCKED".equalsIgnoreCase(newStatus)) {
            refreshTokenRepository.deleteByUserId(user.getId());
        }
    }

    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .status(user.getStatus())
                .build();
    }
}
