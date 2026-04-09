package com.ecommerce.security.oauth2;

import com.ecommerce.entity.User;
import com.ecommerce.security.JwtUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;

    @Value("${cors.allowed-origins:http://localhost:3000}")
    private String allowedOrigins;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        // Lấy thông tin user
        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        User user = oAuth2User.getUser();

        // Tạo JWT Tokens
        String accessToken = jwtUtil.generateAccessToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());

        // Lấy gốc cấu hình ReactJS (http://localhost:3000)
        // Nếu có nhiều domain, lấy domain đầu tiên
        String frontendUrl = allowedOrigins.split(",")[0];

        // Lấy thông tin phụ
        String rolesStr = String.join(",", user.getRoles().stream().map(r -> r.getName()).toList());
        String avatarUrl = user.getAvatarUrl() != null ? user.getAvatarUrl() : "";
        String fullName = user.getFullName() != null ? user.getFullName() : "";

        // Chuyển hướng về ReactJS route: /oauth2/redirect
        String targetUrl = UriComponentsBuilder.fromUriString(frontendUrl + "/oauth2/redirect")
                .queryParam("accessToken", accessToken)
                .queryParam("refreshToken", refreshToken)
                .queryParam("userId", user.getId())
                .queryParam("email", URLEncoder.encode(user.getEmail(), StandardCharsets.UTF_8))
                .queryParam("fullName", URLEncoder.encode(fullName, StandardCharsets.UTF_8))
                .queryParam("avatarUrl", URLEncoder.encode(avatarUrl, StandardCharsets.UTF_8))
                .queryParam("roles", rolesStr)
                .build().toUriString();

        log.info("Xác thực OAuth2 thành công cho user {}, redirect tới {}", user.getEmail(), frontendUrl);

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
