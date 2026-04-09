package com.ecommerce.controller.admin;

import com.ecommerce.dto.response.ApiResponse;
import com.ecommerce.entity.AuditLog;
import com.ecommerce.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@RequiredArgsConstructor
public class AuditLogAdminController {

    private final AuditLogService auditLogService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<AuditLog>> getAllLogs() {
        return ApiResponse.success(auditLogService.getAllLogs(), "Lấy danh sách nhật ký hệ thống thành công");
    }
}
