package com.ecommerce.dto.request.product;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CreateProductRequest {
    @NotBlank(message = "Ten san pham khong duoc de trong")
    @Size(min = 10, max = 200, message = "Ten san pham phai tu 10-200 ky tu")
    private String name;

    @NotNull(message = "Vui long chon danh muc")
    private Integer categoryId;

    @NotBlank(message = "Vui long nhap mo ta san pham")
    private String description;

    @NotNull(message = "Gia san pham khong duoc de trong")
    @DecimalMin(value = "0.0", inclusive = false, message = "Gia san pham phai lon hon 0")
    @DecimalMax(value = "1000000000", message = "Gia khong duoc vuot qua 1 ty VND")
    private BigDecimal basePrice;

    private String status;

    @NotEmpty(message = "Moi san pham can it nhat 1 hinh anh")
    private List<String> imageUrls;

    @Valid
    private List<CreateVariantRequest> variants;

    private Boolean flashSaleEnabled;

    @DecimalMin(value = "0.0", inclusive = false, message = "Gia flash sale phai lon hon 0")
    private BigDecimal flashSalePrice;

    private LocalDateTime flashSaleStartAt;

    private LocalDateTime flashSaleEndAt;
}
