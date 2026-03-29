package com.ecommerce.repository;

import com.ecommerce.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {

    /**
     * Tìm role theo tên.
     * Dùng khi đăng ký: gán ROLE_CUSTOMER mặc định cho user mới.
     * 
     * Ví dụ: roleRepository.findByName("ROLE_CUSTOMER")
     */
    Optional<Role> findByName(String name);
}
