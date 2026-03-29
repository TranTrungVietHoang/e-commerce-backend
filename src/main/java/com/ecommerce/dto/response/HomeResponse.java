package com.ecommerce.dto.response;

import lombok.Data;
import java.util.List;
import com.ecommerce.dto.response.product.ProductResponse;

@Data
public class HomeResponse {
    private List<BannerResponse> banners;
    private List<ProductResponse> newestProducts;
    private List<ProductResponse> bestSellingProducts;
}
