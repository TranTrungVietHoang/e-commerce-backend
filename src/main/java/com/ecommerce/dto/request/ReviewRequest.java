package com.ecommerce.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReviewRequest {
    
    @NotNull(message = "Order Item ID không được để trống")
    private Long orderItemId;

    @NotNull(message = "Rating không được để trống")
    @Min(value = 1, message = "Rating nhỏ nhất là 1")
    @Max(value = 5, message = "Rating lớn nhất là 5")
    private Integer rating;

    @Size(max = 1000, message = "Bình luận (comment) dài tối đa 1000 ký tự")
    private String comment;
}
