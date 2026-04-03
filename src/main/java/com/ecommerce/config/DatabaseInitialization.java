package com.ecommerce.config;

import com.ecommerce.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class DatabaseInitialization implements CommandLineRunner {

    private final ProductRepository productRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        log.info("--- STARTING DATABASE DATA FIX (Member 6 Task) ---");

        // 1. Fix Product Status: Set to 'ACTIVE' if status is NULL or not 'ACTIVE' for existing products
        try {
            long activeCount = productRepository.countByStatus("ACTIVE");
            log.info("Current active products: {}", activeCount);

            if (activeCount == 0) {
                int updated = jdbcTemplate.update("UPDATE products SET status = 'ACTIVE' WHERE status IS NULL OR status != 'ACTIVE'");
                log.info("Fixed status for {} products to 'ACTIVE'", updated);
            }
        } catch (Exception e) {
            log.error("Failed to fix product statuses: {}", e.getMessage());
        }

        // 2. Ensure Slug uniqueness if needed (but Hibernate should handle that via schema)
        
        // 3. Fix Shop status if needed
        try {
            int updatedShops = jdbcTemplate.update("UPDATE shops SET status = 'ACTIVE' WHERE status IS NULL OR status = ''");
            if (updatedShops > 0) {
                log.info("Fixed status for {} shops to 'ACTIVE'", updatedShops);
            }
        } catch (Exception e) {
            log.warn("Shops table may not have status column yet: {}", e.getMessage());
        }

        // 4. CRITICAL FIX: Ensure test user accounts have roles assigned
        try {
            fixUserRoles();
        } catch (Exception e) {
            log.error("Failed to fix user roles: {}", e.getMessage());
        }

        // 5. CRITICAL FIX: Ensure sellers have shops assigned
        try {
            fixSellerShops();
        } catch (Exception e) {
            log.error("Failed to fix seller shops: {}", e.getMessage());
        }

        log.info("--- DATABASE DATA FIX COMPLETE ---");
    }

    /**
     * Ensure test user accounts have proper roles assigned
     * This is critical for login/authorization to work correctly
     */
    private void fixUserRoles() {
        try {
            log.info("Checking and fixing user roles...");

            // Get role IDs
            Integer customerRoleId = jdbcTemplate.queryForObject(
                "SELECT id FROM roles WHERE name = 'ROLE_CUSTOMER'", Integer.class);
            Integer sellerRoleId = jdbcTemplate.queryForObject(
                "SELECT id FROM roles WHERE name = 'ROLE_SELLER'", Integer.class);
            Integer adminRoleId = jdbcTemplate.queryForObject(
                "SELECT id FROM roles WHERE name = 'ROLE_ADMIN'", Integer.class);

            log.info("Role IDs - CUSTOMER: {}, SELLER: {}, ADMIN: {}", customerRoleId, sellerRoleId, adminRoleId);

            // Fix seller accounts (tam18042021@gmail.com)
            Long sellerId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email = 'tam18042021@gmail.com'", Long.class);
            
            // Remove existing roles
            jdbcTemplate.update("DELETE FROM user_roles WHERE user_id = ?", sellerId);
            
            // Assign SELLER role
            jdbcTemplate.update(
                "INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", 
                sellerId, sellerRoleId);
            
            log.info("✓ Assigned ROLE_SELLER to tam18042021@gmail.com (ID: {})", sellerId);

            // Fix other test accounts if they exist
            try {
                Long customerId = jdbcTemplate.queryForObject(
                    "SELECT id FROM users WHERE email IN ('customer@gmail.com', 'User@gmail.com', 'customer1@gmail.com')", 
                    Long.class);
                jdbcTemplate.update("DELETE FROM user_roles WHERE user_id = ?", customerId);
                jdbcTemplate.update(
                    "INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", 
                    customerId, customerRoleId);
                log.info("✓ Assigned ROLE_CUSTOMER to customer account (ID: {})", customerId);
            } catch (Exception e) {
                log.debug("No customer account found with common names");
            }

            // Fix admin account if it exists
            try {
                Long adminId = jdbcTemplate.queryForObject(
                    "SELECT id FROM users WHERE email IN ('admin@gmail.com')", 
                    Long.class);
                jdbcTemplate.update("DELETE FROM user_roles WHERE user_id = ?", adminId);
                jdbcTemplate.update(
                    "INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", 
                    adminId, adminRoleId);
                log.info("✓ Assigned ROLE_ADMIN to admin@gmail.com (ID: {})", adminId);
            } catch (Exception e) {
                log.debug("No admin account found");
            }

            log.info("User roles fix complete!");
            
        } catch (Exception e) {
            log.warn("Could not fix user roles (users might not exist yet): {}", e.getMessage());
        }
    }

    /**
     * Ensure sellers have shops assigned
     * This is critical for seller inventory/dashboard to work correctly
     */
    private void fixSellerShops() {
        try {
            log.info("Checking and creating shops for sellers...");

            // Get seller role ID
            Integer sellerRoleId = jdbcTemplate.queryForObject(
                "SELECT id FROM roles WHERE name = 'ROLE_SELLER'", Integer.class);

            // Find all sellers using native SQL
            java.util.List<Long> sellerIds = jdbcTemplate.queryForList(
                "SELECT DISTINCT u.id FROM users u JOIN user_roles ur ON u.id = ur.user_id " +
                "WHERE ur.role_id = ? AND NOT EXISTS (SELECT 1 FROM shops WHERE seller_id = u.id)",
                new Object[]{sellerRoleId}, Long.class);

            for (Long sellerId : sellerIds) {
                try {
                    // Get seller email for logging
                    String email = jdbcTemplate.queryForObject(
                        "SELECT email FROM users WHERE id = ?", 
                        new Object[]{sellerId}, String.class);

                    // Create a default shop for this seller
                    String shopName = "Cửa hàng của " + email.split("@")[0];
                    jdbcTemplate.update(
                        "INSERT INTO shops (seller_id, name, description, status, rating, created_at, updated_at) " +
                        "VALUES (?, ?, ?, 'ACTIVE', 5.0, GETDATE(), GETDATE())",
                        sellerId, shopName, "Cửa hàng của " + email);
                    
                    log.info("✓ Created shop for seller {} (ID: {})", email, sellerId);
                } catch (Exception e) {
                    log.warn("Could not create shop for seller {}: {}", sellerId, e.getMessage());
                }
            }

            log.info("Seller shops fix complete!");
            
        } catch (Exception e) {
            log.warn("Could not fix seller shops: {}", e.getMessage());
        }
    }
}
