package com.interviewcoach.aiusage.service;

import com.interviewcoach.aiusage.entity.AiUsageLog;
import com.interviewcoach.aiusage.repository.AiUsageLogRepository;
import com.interviewcoach.common.error.AiTokenQuotaExceededException;
import com.interviewcoach.user.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * 平台 AI 月度 token 配额检查服务。
 * 仅对平台默认 AI 调用生效，用户自定义 Provider 不受配额限制。
 */
@Service
public class AiTokenQuotaService {

    private final AiUsageLogRepository usageRepository;

    public AiTokenQuotaService(AiUsageLogRepository usageRepository) {
        this.usageRepository = usageRepository;
    }

    /**
     * 检查用户当前月平台 AI token 用量是否超限。
     * 配额为 null 表示不限制；配额为 0 表示禁止调用。
     *
     * @param user 当前用户
     * @throws AiTokenQuotaExceededException 当用量 >= 配额时抛出
     */
    @Transactional(readOnly = true)
    public void checkPlatformQuota(User user) {
        Long quota = user.getMonthlyTokenQuota();
        if (quota == null) {
            return; // 无配额限制
        }
        long totalTokens = getCurrentMonthPlatformTokens(user.getId());

        if (totalTokens >= quota) {
            throw new AiTokenQuotaExceededException(
                    "Monthly platform AI token quota exceeded. used=" + totalTokens + " quota=" + quota);
        }
    }

    /**
     * 获取用户当前月平台 AI token 用量（SQL 聚合，不加载日志到内存）。
     */
    @Transactional(readOnly = true)
    public long getCurrentMonthPlatformTokens(UUID userId) {
        Instant monthStart = LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1)
                .atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant now = Instant.now();
        return usageRepository.sumTotalTokensByUserIdAndProviderTypeAndCreatedAtBetween(
                userId, "platformDefault", monthStart, now);
    }
}
