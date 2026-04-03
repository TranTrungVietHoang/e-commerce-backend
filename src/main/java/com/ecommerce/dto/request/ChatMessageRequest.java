package com.ecommerce.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChatMessageRequest {
    
    @NotNull(message = "Người nhận (receiverId) không được để trống")
    private Long receiverId;
    
    private Long shopId;

    @NotBlank(message = "Nội dung tin nhắn không được để trống")
    @Size(max = 1000, message = "Tin nhắn chat dài tối đa 1000 ký tự")
    private String message;
}
