package com.ecommerce.repository;

import com.ecommerce.entity.FlashSaleProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FlashSaleProductRepository extends JpaRepository<FlashSaleProduct, Long> {
    
    @Query("SELECT fsp FROM FlashSaleProduct fsp " +
           "JOIN fsp.flashSale fs " +
           "WHERE fs.status = 'ACTIVE' AND fsp.product.id = :productId")
    Optional<FlashSaleProduct> findActiveByProductId(Long productId);

    @Query("SELECT fsp FROM FlashSaleProduct fsp " +
           "JOIN FETCH fsp.product p " +
           "WHERE fsp.flashSale.id = :flashSaleId")
    List<FlashSaleProduct> findByFlashSaleId(Long flashSaleId);

    List<FlashSaleProduct> findByProduct_Shop_Id(Long shopId);
}
