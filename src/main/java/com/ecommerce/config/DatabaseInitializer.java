package com.ecommerce.config;

import com.ecommerce.entity.Role;
import com.ecommerce.entity.Shop;
import com.ecommerce.entity.User;
import com.ecommerce.enums.ShopStatus;
import com.ecommerce.repository.RoleRepository;
import com.ecommerce.repository.ShopRepository;
import com.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile("!prod")
public class DatabaseInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final ShopRepository shopRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        log.info("Bắt đầu kiểm tra và khởi tạo dữ liệu mặc định...");
        
        initializeRoles();
        initializeAdminUser();
        initializeSellerUser();
        
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

    private void initializeSellerUser() {
        String sellerEmail = "seller@gmail.com";
        if (userRepository.findByEmail(sellerEmail).isEmpty()) {
            Role sellerRole = roleRepository.findByName("ROLE_SELLER")
                    .orElseThrow(() -> new RuntimeException("Error: Role SELLER is not found."));

            User seller = User.builder()
                    .email(sellerEmail)
                    .password(passwordEncoder.encode("seller123"))
                    .fullName("Default Seller")
                    .status("ACTIVE")
                    .roles(Collections.singleton(sellerRole))
                    .build();

            userRepository.save(seller);
            log.info("Đã tạo tài khoản Seller mặc định: {} / seller123", sellerEmail);

            // Tạo Shop mặc định cho seller này
            if (shopRepository.findBySellerId(seller.getId()).isEmpty()) {
                Shop shop = Shop.builder()
                        .seller(seller)
                        .name("Cửa hàng MacDinh")
                        .description("Cửa hàng demo được tạo mặc định")
                        .status(ShopStatus.ACTIVE)
                        .rating(BigDecimal.valueOf(5.0))
                        .build();
                shopRepository.save(shop);
                log.info("Đã tạo Shop mặc định cho Seller: {}", sellerEmail);
            }
        }
    }
}
