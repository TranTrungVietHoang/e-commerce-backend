package com.ecommerce.dto.request.product;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class UpdateProductRequest {
    @Size(min = 10, max = 200, message = "Tên sản phẩm phải từ 10-200 ký tự")
    private String name;

    private Integer categoryId;

    private String description;

    @DecimalMin(value = "0.0", inclusive = false, message = "Giá sản phẩm phải lớn hơn 0")
    @DecimalMax(value = "1000000000", message = "Giá không được vượt quá 1 tỷ VNĐ")
    private BigDecimal basePrice;

    private List<String> imageUrls;

    @Valid
    private List<CreateVariantRequest> variants;
}
