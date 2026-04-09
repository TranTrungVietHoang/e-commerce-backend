package com.ecommerce.repository;

import com.ecommerce.entity.FlashSale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FlashSaleRepository extends JpaRepository<FlashSale, Long> {
    
    @Query("SELECT fs FROM FlashSale fs WHERE fs.status = 'ACTIVE' AND fs.startTime <= :now AND fs.endTime >= :now")
    Optional<FlashSale> findCurrentActive(LocalDateTime now);

    List<FlashSale> findAllByOrderByStartTimeDesc();
}
