package com.ecommerce.security;

import com.ecommerce.exception.AppException;
import com.ecommerce.exception.ErrorCode;
import com.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Cầu nối giữa Spring Security và Database.
 * 
 * Spring Security gọi loadUserByUsername() trong 2 tình huống:
 *   1. Khi authenticationManager.authenticate() (lúc Login).
 *   2. Khi JwtAuthenticationFilter resolve token → load user để set SecurityContext.
 * 
 * "username" trong hệ thống này = EMAIL (không phải username thông thường).
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Tìm user theo email — trả về entity User (đã implements UserDetails)
        return userRepository.findFirstByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }
}
