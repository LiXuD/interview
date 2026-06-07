package com.interviewcoach.admin.controller;

import com.interviewcoach.admin.service.AdminUserQuotaService;
import com.interviewcoach.common.api.AdminTokenQuotaDto;
import com.interviewcoach.common.api.AdminTokenQuotaUpdateRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 管理端用户管理 API。所有端点要求 ROLE_ADMIN。
 */
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserQuotaService quotaService;

    public AdminUserController(AdminUserQuotaService quotaService) {
        this.quotaService = quotaService;
    }

    /**
     * 更新用户月度平台 AI token 配额。null 不限制，0 禁止平台 AI。
     */
    @PatchMapping("/{userId}/token-quota")
    public ResponseEntity<AdminTokenQuotaDto> updateTokenQuota(
            @PathVariable UUID userId,
            @RequestBody AdminTokenQuotaUpdateRequest request) {
        return ResponseEntity.ok(quotaService.updateQuota(userId, request.monthlyTokenQuota()));
    }

    /**
     * 查询用户月度平台 AI token 配额状态。
     */
    @GetMapping("/{userId}/token-quota")
    public ResponseEntity<AdminTokenQuotaDto> getTokenQuota(@PathVariable UUID userId) {
        return ResponseEntity.ok(quotaService.getQuota(userId));
    }
}
