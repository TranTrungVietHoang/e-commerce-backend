package com.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "flash_sale_products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlashSaleProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flash_sale_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties("products") // Bỏ qua mảng products của FlashSale
    private FlashSale flashSale;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"shop", "category", "images", "variants"}) // Giảm bớt tải DTO
    private Product product;

    @Column(name = "flash_sale_price", nullable = false, precision = 18, scale = 2)
    private BigDecimal flashSalePrice;

    @Column(name = "slots", nullable = false)
    private Integer slots; // Số lượng đăng ký tham gia Flash Sale

    @Builder.Default
    @Column(name = "sold_count")
    private Integer soldCount = 0; // Số lượng thực tế đã bán trong đợt sale
}
