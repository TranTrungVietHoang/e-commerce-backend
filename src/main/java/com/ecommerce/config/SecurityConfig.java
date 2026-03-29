package com.ecommerce.config;

import com.ecommerce.security.CustomAccessDeniedHandler;
import com.ecommerce.security.CustomAuthenticationEntryPoint;
import com.ecommerce.security.JwtAuthenticationFilter;
import com.ecommerce.security.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Cấu hình bảo mật trung tâm của hệ thống.
 *
 * @EnableWebSecurity: Kích hoạt Spring Security.
 * @EnableMethodSecurity: Cho phép dùng @PreAuthorize, @Secured trên method.
 *
 * KIẾN TRÚC BẢO MẬT:
 *   Request → CORS → JwtAuthenticationFilter → SecurityFilterChain → Controller
 *
 * STATELESS: Không dùng session/cookie → mọi request phải kèm token.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserDetailsServiceImpl userDetailsService;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;

    /**
     * Định nghĩa luật bảo mật cho từng endpoint.
     *
     * LUẬT PHÂN QUYỀN (từ cụ thể → chung):
     *   /api/v1/auth/**     → Public (không cần token)
     *   GET /products/**    → Public (khách vãng lai xem sản phẩm)
     *   GET /categories/**  → Public
     *   /api/v1/admin/**    → Chỉ ADMIN
     *   Còn lại            → Phải đăng nhập
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. Tắt CSRF (dùng JWT không cần CSRF)
                .csrf(AbstractHttpConfigurer::disable)

                // 2. Cấu hình CORS (cho phép FE localhost:3000 gọi API)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 3. Không dùng session (STATELESS)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 4. Quy tắc phân quyền endpoint
                .authorizeHttpRequests(auth -> auth
                        // ── PUBLIC ──────────────────────────────────────
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/products/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/categories/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/shops/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/home/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/search/**").permitAll()
                        // Swagger UI (dev env)
                        .requestMatchers("/swagger-ui/**", "/api-docs/**", "/v3/api-docs/**").permitAll()
                        // Test endpoint
                        .requestMatchers("/api/v1/test/**").permitAll()

                        // ── ADMIN ONLY ──────────────────────────────────
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                        // ── AUTHENTICATED ───────────────────────────────
                        .anyRequest().authenticated()
                )

                // 5. Ngoại lệ bảo mật (401, 403) từ tầng Filter
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(customAuthenticationEntryPoint)
                        .accessDeniedHandler(customAccessDeniedHandler)
                )

                // 6. Dùng AuthenticationProvider (DaoAuthenticationProvider)
                .authenticationProvider(authenticationProvider())

                // 7. Đặt JwtFilter TRƯỚC filter xác thực mặc định của Spring
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "http://localhost:5173"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
