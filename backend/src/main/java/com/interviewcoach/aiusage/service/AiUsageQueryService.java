package com.interviewcoach.aiusage.service;

import com.interviewcoach.aiusage.entity.AiUsageLog;
import com.interviewcoach.aiusage.repository.AiUsageLogRepository;
import com.interviewcoach.common.api.AiUsageBreakdownDto;
import com.interviewcoach.common.api.AiUsageDailyPointDto;
import com.interviewcoach.common.api.AiUsageSummaryDto;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;
import java.util.function.Function;

@Service
public class AiUsageQueryService {

    private final AiUsageLogRepository repository;

    public AiUsageQueryService(AiUsageLogRepository repository) {
        this.repository = repository;
    }

    public AiUsageSummaryDto summary(UUID userId, Instant startAt, Instant endAt) {
        return accumulator(load(userId, startAt, endAt)).toSummary();
    }

    public List<AiUsageDailyPointDto> daily(UUID userId, Instant startAt, Instant endAt) {
        Map<LocalDate, UsageAccumulator> byDay = new TreeMap<>();
        for (AiUsageLog log : load(userId, startAt, endAt)) {
            LocalDate day = LocalDateTime.ofInstant(log.getCreatedAt(), ZoneOffset.UTC).toLocalDate();
            byDay.computeIfAbsent(day, ignored -> new UsageAccumulator()).add(log);
        }
        return byDay.entrySet().stream()
                .map(entry -> entry.getValue().toDaily(entry.getKey().toString()))
                .toList();
    }

    public List<AiUsageBreakdownDto> byTask(UUID userId, Instant startAt, Instant endAt) {
        return breakdown(userId, startAt, endAt, AiUsageLog::getTask);
    }

    public List<AiUsageBreakdownDto> byModel(UUID userId, Instant startAt, Instant endAt) {
        return breakdown(userId, startAt, endAt, AiUsageLog::getModel);
    }

    public List<AiUsageBreakdownDto> byProvider(UUID userId, Instant startAt, Instant endAt) {
        return breakdown(userId, startAt, endAt, AiUsageLog::getProviderType);
    }

    private List<AiUsageBreakdownDto> breakdown(UUID userId,
                                                Instant startAt,
                                                Instant endAt,
                                                Function<AiUsageLog, String> classifier) {
        Map<String, UsageAccumulator> grouped = new TreeMap<>();
        for (AiUsageLog log : load(userId, startAt, endAt)) {
            String name = classifier.apply(log);
            if (name == null || name.isBlank()) {
                name = "unknown";
            }
            grouped.computeIfAbsent(name, ignored -> new UsageAccumulator()).add(log);
        }
        return grouped.entrySet().stream()
                .map(entry -> entry.getValue().toBreakdown(entry.getKey()))
                .toList();
    }

    private List<AiUsageLog> load(UUID userId, Instant startAt, Instant endAt) {
        Instant start = startAt == null ? Instant.EPOCH : startAt;
        Instant end = endAt == null ? Instant.now().plusSeconds(1) : endAt;
        return repository.findByUserIdAndCreatedAtBetweenOrderByCreatedAtAsc(userId, start, end);
    }

    private static UsageAccumulator accumulator(List<AiUsageLog> logs) {
        UsageAccumulator acc = new UsageAccumulator();
        logs.forEach(acc::add);
        return acc;
    }

    private static final class UsageAccumulator {
        private long totalRequests;
        private long successfulRequests;
        private long failedRequests;
        private long estimatedRequests;
        private long totalInputTokens;
        private long totalOutputTokens;
        private long totalCacheCreationTokens;
        private long totalCacheReadTokens;
        private long totalReasoningTokens;
        private long totalTokens;

        void add(AiUsageLog log) {
            totalRequests++;
            if (log.isSuccess()) {
                successfulRequests++;
            } else {
                failedRequests++;
            }
            if (log.isEstimated()) {
                estimatedRequests++;
            }
            totalInputTokens += log.getInputTokens();
            totalOutputTokens += log.getOutputTokens();
            totalCacheCreationTokens += log.getCacheCreationTokens();
            totalCacheReadTokens += log.getCacheReadTokens();
            totalReasoningTokens += log.getReasoningTokens();
            totalTokens += log.getTotalTokens();
        }

        AiUsageSummaryDto toSummary() {
            return new AiUsageSummaryDto(
                    totalRequests,
                    successfulRequests,
                    failedRequests,
                    estimatedRequests,
                    totalInputTokens,
                    totalOutputTokens,
                    totalCacheCreationTokens,
                    totalCacheReadTokens,
                    totalReasoningTokens,
                    totalTokens);
        }

        AiUsageDailyPointDto toDaily(String date) {
            return new AiUsageDailyPointDto(
                    date,
                    totalRequests,
                    successfulRequests,
                    failedRequests,
                    totalInputTokens,
                    totalOutputTokens,
                    totalCacheCreationTokens,
                    totalCacheReadTokens,
                    totalReasoningTokens,
                    totalTokens);
        }

        AiUsageBreakdownDto toBreakdown(String name) {
            return new AiUsageBreakdownDto(
                    name,
                    totalRequests,
                    successfulRequests,
                    failedRequests,
                    estimatedRequests,
                    totalInputTokens,
                    totalOutputTokens,
                    totalCacheCreationTokens,
                    totalCacheReadTokens,
                    totalReasoningTokens,
                    totalTokens);
        }
    }
}
