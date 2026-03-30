package com.ecommerce.service;

import com.ecommerce.dto.request.auth.LoginRequest;
import com.ecommerce.dto.request.auth.RefreshTokenRequest;
import com.ecommerce.dto.request.auth.RegisterRequest;
import com.ecommerce.dto.response.auth.AuthResponse;

public interface AuthService {
    // Trả về AuthResponse để khớp với ApiResponse<AuthResponse> trong Controller
    AuthResponse register(RegisterRequest request);
    
    AuthResponse login(LoginRequest request);
    
    AuthResponse refreshToken(RefreshTokenRequest request);
    
    // Controller đang truyền userDetails.getUsername() (là String)
    void logout(String username); 

    // Các hàm bổ trợ khác (nếu cần dùng ở chỗ khác)
    void verifyEmail(String token);
    void forgotPassword(String email);
    void resetPassword(String token, String newPassword);
}