package com.ecommerce.repository;

import com.ecommerce.entity.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Long> {
    Optional<Voucher> findByCodeIgnoreCase(String code);
    List<Voucher> findByShopIdOrShopIsNull(Long shopId);
}
