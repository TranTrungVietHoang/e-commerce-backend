package com.ecommerce.dto.response.product;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchSuggestionResponse {
    private Long id;
    private String name;
    private String slug;
    private String thumbnailUrl;
}
