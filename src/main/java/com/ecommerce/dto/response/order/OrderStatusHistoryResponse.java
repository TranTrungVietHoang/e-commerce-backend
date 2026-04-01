package com.ecommerce.dto.response.order;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
public class OrderStatusHistoryResponse implements Serializable {
    private Long id;
    private String status;
    @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    private LocalDateTime changedAt;
    private String note;
}
