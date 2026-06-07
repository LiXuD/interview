package com.interviewcoach.aiusage;

import com.interviewcoach.aiusage.entity.AiUsageLog;
import com.interviewcoach.aiusage.repository.AiUsageLogRepository;
import com.interviewcoach.aiusage.service.AiTokenQuotaService;
import com.interviewcoach.common.error.AiTokenQuotaExceededException;
import com.interviewcoach.user.entity.User;
import com.interviewcoach.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class AiTokenQuotaServiceTest {

    @Autowired private AiTokenQuotaService quotaService;
    @Autowired private UserRepository userRepository;
    @Autowired private AiUsageLogRepository usageRepository;

    private User user;

    @BeforeEach
    void setUp() {
        usageRepository.deleteAll();
        // 不删除所有用户，避免 FK 约束冲突；仅查找或创建测试专用用户
        user = userRepository.findByUsername("quotatest_" + getClass().getSimpleName())
                .orElseGet(() -> {
                    User u = new User();
                    u.setUsername("quotatest_" + getClass().getSimpleName());
                    return userRepository.save(u);
                });
    }

    @Test
    void nullQuotaAllowsUnlimited() {
        user.setMonthlyTokenQuota(null);
        userRepository.save(user);
        assertDoesNotThrow(() -> quotaService.checkPlatformQuota(user));
    }

    @Test
    void zeroQuotaBlocksAllPlatformCalls() {
        user.setMonthlyTokenQuota(0L);
        userRepository.save(user);
        assertThrows(AiTokenQuotaExceededException.class, () -> quotaService.checkPlatformQuota(user));
    }

    @Test
    void quotaNotExceededAllowsCalls() {
        user.setMonthlyTokenQuota(10000L);
        userRepository.save(user);

        insertUsageLog(5000, "platformDefault");
        assertDoesNotThrow(() -> quotaService.checkPlatformQuota(user));
    }

    @Test
    void quotaExceededBlocksCalls() {
        user.setMonthlyTokenQuota(1000L);
        userRepository.save(user);

        insertUsageLog(1000, "platformDefault");
        assertThrows(AiTokenQuotaExceededException.class, () -> quotaService.checkPlatformQuota(user));
    }

    @Test
    void userProviderUsageDoesNotCountAgainstQuota() {
        user.setMonthlyTokenQuota(100L);
        userRepository.save(user);

        insertUsageLog(5000, "userOpenAICompatible");
        assertDoesNotThrow(() -> quotaService.checkPlatformQuota(user));
    }

    @Test
    void getCurrentMonthPlatformTokensSumsCorrectly() {
        insertUsageLog(100, "platformDefault");
        insertUsageLog(200, "platformDefault");
        insertUsageLog(500, "userOpenAICompatible");

        long tokens = quotaService.getCurrentMonthPlatformTokens(user.getId());
        assertEquals(300, tokens);
    }

    private void insertUsageLog(int totalTokens, String providerType) {
        AiUsageLog log = new AiUsageLog();
        log.setUserId(user.getId());
        log.setRequestId("req-" + System.nanoTime());
        log.setTask("test");
        log.setProviderType(providerType);
        log.setModel("test-model");
        log.setMode("chatCompletions");
        log.setUsageSource("actual");
        log.setTotalTokens(totalTokens);
        log.setSuccess(true);
        usageRepository.save(log);
    }
}
