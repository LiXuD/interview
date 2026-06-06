package com.interviewcoach.aiusage.service;

import com.interviewcoach.aiusage.entity.AiUsageLog;
import com.interviewcoach.aiusage.repository.AiUsageLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 持久化 AI usage。写入失败不应阻断用户的核心 AI 流程。
 */
@Service
public class PersistentAiUsageRecorder implements AiUsageRecorder {

    private static final Logger log = LoggerFactory.getLogger(PersistentAiUsageRecorder.class);

    private final AiUsageLogRepository repository;

    public PersistentAiUsageRecorder(AiUsageLogRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(AiUsageLogCommand command) {
        if (command == null || command.userId() == null) {
            return;
        }
        try {
            repository.saveAndFlush(toEntity(command));
        } catch (Exception ex) {
            log.warn("AI usage record failed. requestId={} task={} provider={} model={}",
                    command.requestId(), command.task(), command.providerType(), command.model(), ex);
        }
    }

    private static AiUsageLog toEntity(AiUsageLogCommand command) {
        AiUsageLog log = new AiUsageLog();
        log.setUserId(command.userId());
        log.setTargetId(command.targetId());
        log.setRequestId(safe(command.requestId()));
        log.setTask(safe(command.task()));
        log.setProviderType(safe(command.providerType()));
        log.setProviderId(command.providerId());
        log.setModel(safe(command.model()));
        log.setMode(safe(command.mode()));
        log.setUsageSource(safe(command.usageSource()));
        log.setInputTokens(Math.max(0, command.inputTokens()));
        log.setOutputTokens(Math.max(0, command.outputTokens()));
        log.setCacheCreationTokens(Math.max(0, command.cacheCreationTokens()));
        log.setCacheReadTokens(Math.max(0, command.cacheReadTokens()));
        log.setReasoningTokens(Math.max(0, command.reasoningTokens()));
        log.setTotalTokens(Math.max(0, command.totalTokens()));
        log.setEstimated(command.estimated());
        log.setSuccess(command.success());
        log.setParseFailed(command.parseFailed());
        log.setValidationFailed(command.validationFailed());
        log.setTimeout(command.timeout());
        log.setRetryCount(Math.max(0, command.retryCount()));
        log.setDurationMs(Math.max(0, command.durationMs()));
        return log;
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }
}
