package com.interviewcoach.admin.service;

import com.interviewcoach.aiusage.service.AiTokenQuotaService;
import com.interviewcoach.common.api.AdminTokenQuotaDto;
import com.interviewcoach.common.error.UserNotFoundException;
import com.interviewcoach.user.entity.User;
import com.interviewcoach.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 管理端用户配额管理服务，提供配额更新和配额状态查询。
 */
@Service
public class AdminUserQuotaService {

    private final UserRepository userRepository;
    private final AiTokenQuotaService quotaService;

    public AdminUserQuotaService(UserRepository userRepository, AiTokenQuotaService quotaService) {
        this.userRepository = userRepository;
        this.quotaService = quotaService;
    }

    /**
     * 更新用户的月度平台 AI token 配额。
     * null 表示不限制，0 表示禁止平台 AI 调用。
     *
     * @param userId           目标用户 ID
     * @param monthlyTokenQuota 新的配额值
     * @return 更新后的配额 DTO
     */
    @Transactional
    public AdminTokenQuotaDto updateQuota(UUID userId, Long monthlyTokenQuota) {
        if (monthlyTokenQuota != null && monthlyTokenQuota < 0) {
            throw new IllegalArgumentException("Token quota must not be negative");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        user.setMonthlyTokenQuota(monthlyTokenQuota);
        return buildQuotaDto(user);
    }

    /**
     * 查询用户的配额状态。
     */
    @Transactional(readOnly = true)
    public AdminTokenQuotaDto getQuota(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        return buildQuotaDto(user);
    }

    private AdminTokenQuotaDto buildQuotaDto(User user) {
        long currentMonthTokens = quotaService.getCurrentMonthPlatformTokens(user.getId());
        Long quota = user.getMonthlyTokenQuota();
        Long remaining = quota != null ? Math.max(0, quota - currentMonthTokens) : null;
        boolean exceeded = quota != null && currentMonthTokens >= quota;
        return new AdminTokenQuotaDto(
                user.getId().toString(), quota, currentMonthTokens, remaining, exceeded);
    }
}
