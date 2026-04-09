package com.ecommerce.controller;

import com.ecommerce.dto.request.user.ChangePasswordRequest;
import com.ecommerce.dto.request.user.UpdateProfileRequest;
import com.ecommerce.dto.response.ApiResponse;
import com.ecommerce.dto.response.user.UserResponse;
import com.ecommerce.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<UserResponse>> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                userService.getProfile(userDetails.getUsername()), "Lấy thông tin thành công"));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                userService.updateProfile(userDetails.getUsername(), request), "Cập nhật thông tin thành công"));
    }

    @PutMapping("/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success(null, "Đổi mật khẩu thành công"));
    }

    /**
     * Upload ảnh đại diện lên Cloudinary.
     * FE gửi multipart/form-data với field "file".
     * Trả về { avatarUrl: "https://res.cloudinary.com/..." }
     */
    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadAvatar(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("file") MultipartFile file) {
        String url = userService.uploadAvatar(userDetails.getUsername(), file);
        return ResponseEntity.ok(ApiResponse.success(
                Map.of("avatarUrl", url), "Tải ảnh đại diện thành công"));
    }
}
