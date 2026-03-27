package com.ecommerce.security;

import com.ecommerce.exception.AppException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter chạy TRƯỚC mọi request để xác thực JWT.
 * 
 * OncePerRequestFilter: đảm bảo chỉ chạy 1 lần mỗi request (tránh duplicate).
 * 
 * LUỒNG XỬ LÝ:
 *   Request → [Filter này] → Các filter khác → Controller
 * 
 * LOGIC CHÍNH:
 *   1. Đọc header "Authorization: Bearer <token>"
 *   2. Không có token → bỏ qua (request đến endpoint public)
 *   3. Có token → extract email → load user từ DB → validate token
 *   4. Valid → set Authentication vào SecurityContext → request được phép qua
 *   5. Invalid → SecurityContext trống → Spring Security trả 401
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        // 1. Lấy token từ header
        final String authHeader = request.getHeader("Authorization");

        // 2. Không có token hoặc sai format → bỏ qua, không xử lý
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 3. Tách token (bỏ phần "Bearer ")
            final String jwt = authHeader.substring(7);

            // 4. Extract email từ token (chưa validate, chỉ đọc payload)
            final String email = jwtUtil.extractUsername(jwt);

            // 5. Chỉ xử lý nếu có email VÀ chưa có authentication trong context
            //    (tránh overwrite nếu filter chạy 2 lần)
            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // 6. Load UserDetails từ DB để lấy roles + trạng thái tài khoản
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                // 7. Validate token (chữ ký + hạn dùng + khớp với user)
                if (jwtUtil.isTokenValid(jwt, userDetails)) {
                    // 8. Tạo Authentication object và set vào SecurityContext
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null, // credentials = null (đã xác thực qua token)
                                    userDetails.getAuthorities() // roles
                            );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // 9. Set vào SecurityContext → request được xem là đã xác thực
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    log.debug("Xác thực JWT thành công cho user: {}", email);
                }
            }
        } catch (AppException e) {
            // Token hết hạn hoặc không hợp lệ → log và để SecurityContext trống
            // Spring Security sẽ tự trả 401
            log.warn("JWT không hợp lệ: {}", e.getMessage());
            // Không ném exception ra ngoài filter, để chain tiếp tục
            // AuthenticationEntryPoint của SecurityConfig sẽ xử lý 401
        }

        filterChain.doFilter(request, response);
    }
}
