package com.ecommerce.dto.response.user;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UserResponse {
    private Long id;
    private String email;
    private String fullName;
    private String phone;
    private String status;
    private String avatarUrl;
    private List<String> roles;
}
