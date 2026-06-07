package com.interviewcoach.admin.controller;

import com.interviewcoach.admin.service.AdminAiUsageService;
import com.interviewcoach.common.api.AdminAiUsageOverviewDto;
import com.interviewcoach.common.api.AdminAiUsageUserDetailDto;
import com.interviewcoach.common.api.AdminAiUsageUsersPageDto;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * 管理端 AI 用量查询 API。所有端点要求 ROLE_ADMIN。
 */
@RestController
@RequestMapping("/api/admin/ai-usage")
public class AdminAiUsageController {

    private final AdminAiUsageService adminAiUsageService;

    public AdminAiUsageController(AdminAiUsageService adminAiUsageService) {
        this.adminAiUsageService = adminAiUsageService;
    }

    /**
     * 全局用量概览：总用户、活跃用户、配额超限用户、汇总指标、每日趋势、Top 用户/模型/任务/Provider。
     */
    @GetMapping("/overview")
    public ResponseEntity<AdminAiUsageOverviewDto> overview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(adminAiUsageService.overview(startDate, endDate));
    }

    /**
     * 用户用量分页列表，支持关键字搜索、日期范围和排序。
     */
    @GetMapping("/users")
    public ResponseEntity<AdminAiUsageUsersPageDto> users(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "totalTokensDesc") String sort) {
        if (size > 100) size = 100;
        return ResponseEntity.ok(adminAiUsageService.usersPage(startDate, endDate, keyword, page, size, sort));
    }

    /**
     * 单用户用量详情：个人资料、配额、汇总、每日趋势和按任务/模型/Provider 分解。
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<AdminAiUsageUserDetailDto> userDetail(
            @PathVariable UUID userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(adminAiUsageService.userDetail(userId, startDate, endDate));
    }
}
