package com.ecommerce.repository;

import com.ecommerce.entity.Shop;
import com.ecommerce.entity.User;
import com.ecommerce.enums.ShopStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository cho Shop
 */
@Repository
public interface ShopRepository extends JpaRepository<Shop, Long> {
    
    // 1. Lấy danh sách shop theo trạng thái (Dùng cho Admin lọc shop PENDING để duyệt)
    List<Shop> findByStatus(ShopStatus status);

    // 2. Tìm shop theo đối tượng User (Seller) 
    Optional<Shop> findBySeller(User seller);

    // 3. Tìm shop nhanh bằng ID của người bán (Seller ID)
    Optional<Shop> findBySellerId(Long sellerId);

    // 4. Kiểm tra tên shop đã tồn tại chưa (Để tránh trùng lặp khi đăng ký)
    boolean existsByName(String name);

    // 5. Kiểm tra xem một User đã có Shop chưa (Vì quan hệ là OneToOne)
    boolean existsBySellerId(Long sellerId);

    // 6. Lấy chi tiết Shop kèm thông tin Seller (JOIN FETCH tránh LazyLoad lỗi 500)
    @Query("SELECT s FROM Shop s JOIN FETCH s.seller WHERE s.id = :id")
    Optional<Shop> findByIdWithSeller(@Param("id") Long id);

    @Query("SELECT s FROM Shop s JOIN FETCH s.seller")
    List<Shop> findAllWithSeller();

    @Query("SELECT s FROM Shop s JOIN FETCH s.seller WHERE s.status = :status")
    List<Shop> findByStatusWithSeller(@Param("status") ShopStatus status);
}