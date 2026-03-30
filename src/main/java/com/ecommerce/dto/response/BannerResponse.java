package com.ecommerce.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BannerResponse {
    private String title;
    private String subtitle;
    private String imageUrl;
    private String linkUrl;
}
