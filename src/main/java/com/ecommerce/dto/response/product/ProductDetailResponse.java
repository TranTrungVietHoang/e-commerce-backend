package com.ecommerce.dto.response.product;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class ProductDetailResponse extends ProductResponse {
    private Long categoryId;
    private String description;
    private String slug;
    private Long shopId;
    private String shopName;
    private List<ProductImageResponse> images;
    private List<VariantResponse> variants;
}
