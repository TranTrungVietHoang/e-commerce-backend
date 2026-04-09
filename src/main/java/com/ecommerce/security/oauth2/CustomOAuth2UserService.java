package com.ecommerce.security.oauth2;

import com.ecommerce.entity.Role;
import com.ecommerce.entity.User;
import com.ecommerce.repository.RoleRepository;
import com.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        try {
            return processOAuth2User(userRequest, oAuth2User);
        } catch (Exception ex) {
            log.error("Lỗi khi xử lý OAuth2 User", ex);
            // Throw exception để Spring Security xử lý thất bại đăng nhập
            throw new OAuth2AuthenticationException(ex.getMessage());
        }
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest oAuth2UserRequest, OAuth2User oAuth2User) {
        String email = oAuth2User.getAttribute("email");
        if (email == null) {
            throw new IllegalArgumentException("Không tìm thấy email từ nhà cung cấp OAuth2");
        }

        Optional<User> userOptional = userRepository.findFirstByEmail(email);
        User user;
        if (userOptional.isPresent()) {
            user = userOptional.get();
            // Nếu muốn, có thể cập nhật tên và avatar từ Google mỗi lần đăng nhập
             log.info("Người dùng {} đã đăng nhập thông qua Google.", email);
        } else {
            // Đăng ký người dùng mới với chuỗi mật khẩu ảo
            log.info("Người dùng {} chưa tồn tại. Tiến hành tự động tạo tài khoản.", email);
            user = registerNewOAuth2User(oAuth2User);
        }

        return CustomOAuth2User.create(user, oAuth2User.getAttributes());
    }

    private User registerNewOAuth2User(OAuth2User oAuth2User) {
        String email = oAuth2User.getAttribute("email");
        String fullName = oAuth2User.getAttribute("name");
        String avatarUrl = oAuth2User.getAttribute("picture");

        User user = new User();
        user.setEmail(email);
        user.setFullName(fullName != null ? fullName : "Google User");
        user.setAvatarUrl(avatarUrl);
        // Tạo chuỗi mật khẩu ảo vì họ đăng nhập bằng Google
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setStatus("ACTIVE");

        Role userRole = roleRepository.findByName("ROLE_CUSTOMER")
                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
        user.setRoles(Collections.singleton(userRole));

        return userRepository.save(user);
    }
}
