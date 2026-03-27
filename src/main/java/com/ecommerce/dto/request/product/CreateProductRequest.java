package com.ecommerce.dto.request.product;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CreateProductRequest {
    @NotBlank(message = "Tên sản phẩm không được để trống")
    @Size(min = 10, max = 200, message = "Tên sản phẩm phải từ 10-200 ký tự")
    private String name;

    @NotNull(message = "Vui lòng chọn danh mục")
    private Integer categoryId;

    @NotBlank(message = "Vui lòng nhập mô tả sản phẩm")
    private String description;

    @NotNull(message = "Giá sản phẩm không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Giá sản phẩm phải lớn hơn 0")
    @DecimalMax(value = "1000000000", message = "Giá không được vượt quá 1 tỷ VNĐ")
    private BigDecimal basePrice;

    @NotEmpty(message = "Mỗi sản phẩm cần ít nhất 1 hình ảnh")
    private List<String> imageUrls;

    @Valid
    private List<CreateVariantRequest> variants;
}
