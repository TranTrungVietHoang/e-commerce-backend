package com.ecommerce.dto.request.shop;

import com.ecommerce.enums.ShopStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ShopApprovalRequest {
    @NotNull(message = "Vui lòng chỉ định trạng thái phê duyệt (ví dụ: APPROVED hoặc REJECTED)")
    private ShopStatus status;
    
    // Admin có thể gửi thêm lý do từ chối để sau này hiển thị cho User
    private String reason;
}
