package com.ecommerce.dto.request.shop;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ShopUpdateRequest {
    @Size(min = 5, message = "Tên shop quá ngắn. Yêu cầu ít nhất 5 ký tự")
    private String name;

    private String description;
    
    private String logoUrl;
    
    private String bannerUrl;
}
