package com.ecommerce.dto.request.product;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class UpdateProductRequest {
    @Size(min = 10, max = 200, message = "Ten san pham phai tu 10-200 ky tu")
    private String name;

    private Long categoryId;

    private String description;

    @DecimalMin(value = "0.0", inclusive = false, message = "Gia san pham phai lon hon 0")
    @DecimalMax(value = "1000000000", message = "Gia khong duoc vuot qua 1 ty VND")
    private BigDecimal basePrice;

    private String status;

    private Integer stockQuantity;

    private List<String> imageUrls;

    @Valid
    private List<CreateVariantRequest> variants;

    private Boolean flashSaleEnabled;

    @DecimalMin(value = "0.0", inclusive = false, message = "Gia flash sale phai lon hon 0")
    private BigDecimal flashSalePrice;

    private LocalDateTime flashSaleStartAt;

    private LocalDateTime flashSaleEndAt;
}
