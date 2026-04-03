package com.ecommerce.config;

import com.ecommerce.entity.Role;
import com.ecommerce.entity.User;
import com.ecommerce.entity.Shop;
import com.ecommerce.enums.ShopStatus;
import com.ecommerce.repository.RoleRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.repository.ShopRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Tự động khởi tạo dữ liệu mặc định khi server chạy lần đầu:
 * - Các Role: ROLE_ADMIN, ROLE_SELLER, ROLE_CUSTOMER
 * - Tài khoản Admin: admin@demo.com / Admin@123
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final ShopRepository shopRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        initRoles();
        initAdminUser();
    }

    private void initRoles() {
        createRoleIfNotExists("ROLE_ADMIN");
        createRoleIfNotExists("ROLE_SELLER");
        createRoleIfNotExists("ROLE_CUSTOMER");
        log.info("✅ Đã khởi tạo xong các Role mặc định.");
    }

    private void createRoleIfNotExists(String roleName) {
        if (roleRepository.findByName(roleName).isEmpty()) {
            Role role = new Role();
            role.setName(roleName);
            roleRepository.save(role);
            log.info("  → Đã tạo role: {}", roleName);
        }
    }

    private void initAdminUser() {
        String adminEmail = "admin@demo.com";
        Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseThrow(() -> new RuntimeException("ROLE_ADMIN không tồn tại!"));

        User admin = userRepository.findByEmail(adminEmail).orElse(null);

        if (admin == null) {
            admin = User.builder()
                    .email(adminEmail)
                    .password(passwordEncoder.encode("Admin@123"))
                    .fullName("Admin System")
                    .phone("0999999990")
                    .status("ACTIVE")
                    .roles(new java.util.HashSet<>(Set.of(adminRole)))
                    .build();
            userRepository.save(admin);
            log.info("✅ Đã tạo tài khoản Admin mới: {}", adminEmail);
        } else {
            // Đảm bảo Admin cũ luôn có role ADMIN (Trường hợp bị lỗi DB trước đó)
            if (!admin.getRoles().contains(adminRole)) {
                admin.getRoles().add(adminRole);
                userRepository.save(admin);
                log.info("✅ Đã bổ sung ROLE_ADMIN cho tài khoản admin cũ.");
            }
            // Đảm bảo trạng thái luôn là ACTIVE để không bị 403
            if (!"ACTIVE".equals(admin.getStatus())) {
                admin.setStatus("ACTIVE");
                userRepository.save(admin);
            }
            // THU HỒI ROLE_SELLER của Admin nếu có (Quy tắc mới: Admin không bán hàng)
            Role sellerRole = roleRepository.findByName("ROLE_SELLER").orElse(null);
            if (sellerRole != null && admin.getRoles().contains(sellerRole)) {
                admin.getRoles().remove(sellerRole);
                userRepository.save(admin);
                log.warn("⚠️ Đã thu hồi ROLE_SELLER từ tài khoản admin để tuân thủ quy tắc mới.");
            }
        }

        // Xóa bỏ hoàn toàn gian hàng của Admin (nếu lỡ tồn tại từ phiên bản cũ)
        User finalAdmin = admin;
        shopRepository.findBySeller(finalAdmin).ifPresent(shop -> {
            shopRepository.delete(shop);
            log.warn("⚠️ Đã xóa bỏ gian hàng cũ của Admin ({}) để tuân thủ quy tắc mới.", adminEmail);
        });
    }
}
