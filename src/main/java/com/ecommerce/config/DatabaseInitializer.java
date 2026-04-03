package com.ecommerce.config;

import com.ecommerce.entity.Role;
import com.ecommerce.entity.User;
import com.ecommerce.repository.RoleRepository;
import com.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        log.info("Bắt đầu kiểm tra và khởi tạo dữ liệu mặc định...");
        
        initializeRoles();
        initializeAdminUser();
        
        log.info("Khởi tạo dữ liệu hoàn tất.");
    }

    private void initializeRoles() {
        List<String> defaultRoles = List.of("ROLE_CUSTOMER", "ROLE_SELLER", "ROLE_ADMIN");
        
        for (String roleName : defaultRoles) {
            if (roleRepository.findByName(roleName).isEmpty()) {
                Role role = new Role();
                role.setName(roleName);
                roleRepository.save(role);
                log.info("Đã tạo Role mặc định: {}", roleName);
            }
        }
    }

    private void initializeAdminUser() {
        String adminEmail = "admin@admin.com";
        if (userRepository.findByEmail(adminEmail).isEmpty()) {
            Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                    .orElseThrow(() -> new RuntimeException("Error: Role ADMIN is not found."));

            User admin = User.builder()
                    .email(adminEmail)
                    .password(passwordEncoder.encode("admin123"))
                    .fullName("System Administrator")
                    .status("ACTIVE")
                    .roles(Collections.singleton(adminRole))
                    .build();

            userRepository.save(admin);
            log.info("Đã tạo tài khoản Admin mặc định: {} / admin123", adminEmail);
        }
    }
}
