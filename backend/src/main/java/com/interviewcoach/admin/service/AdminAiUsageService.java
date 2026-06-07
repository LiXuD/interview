package com.interviewcoach.admin.service;

import com.interviewcoach.aiusage.entity.AiUsageLog;
import com.interviewcoach.aiusage.repository.AiUsageLogRepository;
import com.interviewcoach.aiusage.service.UsageAccumulator;
import com.interviewcoach.common.api.*;
import com.interviewcoach.user.entity.User;
import com.interviewcoach.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 管理端 AI 用量聚合服务，提供跨用户的全局概览、用户列表和单用户详情。
 */
@Service
public class AdminAiUsageService {

    private final AiUsageLogRepository usageRepository;
    private final UserRepository userRepository;

    public AdminAiUsageService(AiUsageLogRepository usageRepository, UserRepository userRepository) {
        this.usageRepository = usageRepository;
        this.userRepository = userRepository;
    }

    /**
     * 全局概览：总用户数、活跃用户数、配额超限用户数、汇总指标、每日趋势、Top 用户/模型/任务/Provider。
     */
    @Transactional(readOnly = true)
    public AdminAiUsageOverviewDto overview(LocalDate startDate, LocalDate endDate) {
        Instant start = toStartInstant(startDate);
        Instant end = toEndInstant(endDate);
        List<AiUsageLog> logs = usageRepository.findByCreatedAtBetween(start, end);

        long totalUsers = userRepository.count();
        Set<UUID> activeUserIds = logs.stream().map(AiUsageLog::getUserId).collect(Collectors.toSet());
        long activeUsers = activeUserIds.size();
        long quotaExceededUsers = countQuotaExceededUsers();

        UsageAccumulator globalAcc = new UsageAccumulator();
        Map<UUID, UsageAccumulator> userAccs = new HashMap<>();
        Map<String, UsageAccumulator> modelAccs = new TreeMap<>();
        Map<String, UsageAccumulator> taskAccs = new TreeMap<>();
        Map<String, UsageAccumulator> providerAccs = new TreeMap<>();
        Map<LocalDate, UsageAccumulator> dailyAccs = new TreeMap<>();

        for (AiUsageLog log : logs) {
            globalAcc.add(log);
            userAccs.computeIfAbsent(log.getUserId(), k -> new UsageAccumulator()).add(log);
            modelAccs.computeIfAbsent(safeName(log.getModel()), k -> new UsageAccumulator()).add(log);
            taskAccs.computeIfAbsent(safeName(log.getTask()), k -> new UsageAccumulator()).add(log);
            providerAccs.computeIfAbsent(safeName(log.getProviderType()), k -> new UsageAccumulator()).add(log);
            LocalDate day = LocalDateTime.ofInstant(log.getCreatedAt(), ZoneOffset.UTC).toLocalDate();
            dailyAccs.computeIfAbsent(day, k -> new UsageAccumulator()).add(log);
        }

        List<AiUsageDailyPointDto> daily = dailyAccs.entrySet().stream()
                .map(e -> e.getValue().toDaily(e.getKey().toString()))
                .toList();

        Map<UUID, User> userMap = userRepository.findAllById(userAccs.keySet()).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        List<AiUsageBreakdownDto> topUsers = userAccs.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue().getTotalTokens(), a.getValue().getTotalTokens()))
                .limit(10)
                .map(e -> {
                    User u = userMap.get(e.getKey());
                    String name = u != null ? u.getUsername() : e.getKey().toString();
                    return e.getValue().toBreakdown(name);
                })
                .toList();

        List<AiUsageBreakdownDto> topModels = modelAccs.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue().getTotalTokens(), a.getValue().getTotalTokens()))
                .limit(10)
                .map(e -> e.getValue().toBreakdown(e.getKey()))
                .toList();

        List<AiUsageBreakdownDto> topTasks = taskAccs.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue().getTotalTokens(), a.getValue().getTotalTokens()))
                .limit(10)
                .map(e -> e.getValue().toBreakdown(e.getKey()))
                .toList();

        List<AiUsageBreakdownDto> providers = providerAccs.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue().getTotalTokens(), a.getValue().getTotalTokens()))
                .map(e -> e.getValue().toBreakdown(e.getKey()))
                .toList();

        return new AdminAiUsageOverviewDto(
                totalUsers, activeUsers, quotaExceededUsers,
                globalAcc.toSummary(), daily, topUsers, topModels, topTasks, providers);
    }

    /**
     * 用户用量分页列表，支持关键字搜索和排序。
     */
    @Transactional(readOnly = true)
    public AdminAiUsageUsersPageDto usersPage(LocalDate startDate, LocalDate endDate,
                                               String keyword, int page, int size, String sort) {
        Instant start = toStartInstant(startDate);
        Instant end = toEndInstant(endDate);

        List<User> users;
        if (keyword != null && !keyword.isBlank()) {
            users = userRepository.findByUsernameContainingIgnoreCase(keyword);
        } else {
            users = userRepository.findAll();
        }

        List<UUID> userIds = users.stream().map(User::getId).toList();
        Map<UUID, User> userMap = users.stream().collect(Collectors.toMap(User::getId, Function.identity()));

        List<AiUsageLog> logs = userIds.isEmpty() ? List.of()
                : usageRepository.findByUserIdInAndCreatedAtBetween(userIds, start, end);

        Map<UUID, UsageAccumulator> userAccs = new HashMap<>();
        Map<UUID, Instant> lastUsedMap = new HashMap<>();
        for (AiUsageLog log : logs) {
            userAccs.computeIfAbsent(log.getUserId(), k -> new UsageAccumulator()).add(log);
            lastUsedMap.merge(log.getUserId(), log.getCreatedAt(), (a, b) -> a.isAfter(b) ? a : b);
        }

        List<AdminAiUsageUserRowDto> rows = new ArrayList<>();
        for (User user : users) {
            UsageAccumulator acc = userAccs.getOrDefault(user.getId(), new UsageAccumulator());
            long currentMonthTokens = acc.getTotalTokens();
            Long quota = user.getMonthlyTokenQuota();
            Long remaining = quota != null ? Math.max(0, quota - currentMonthTokens) : null;
            boolean exceeded = quota != null && currentMonthTokens >= quota;
            String lastUsed = lastUsedMap.containsKey(user.getId())
                    ? lastUsedMap.get(user.getId()).toString() : null;

            rows.add(new AdminAiUsageUserRowDto(
                    user.getId().toString(), user.getUsername(), user.getEmail(), user.getRole(),
                    user.getCreatedAt() != null ? user.getCreatedAt().toString() : null,
                    quota, currentMonthTokens, remaining, exceeded, lastUsed,
                    acc.toSummary()));
        }

        Comparator<AdminAiUsageUserRowDto> comparator = switch (sort != null ? sort : "totalTokensDesc") {
            case "totalRequestsDesc" -> Comparator.comparingLong(
                    (AdminAiUsageUserRowDto r) -> r.summary().totalRequests()).reversed();
            case "usernameAsc" -> Comparator.comparing(
                    (AdminAiUsageUserRowDto r) -> r.username() != null ? r.username() : "", String.CASE_INSENSITIVE_ORDER);
            case "createdAtDesc" -> Comparator.comparing(
                    (AdminAiUsageUserRowDto r) -> r.createdAt() != null ? r.createdAt() : "", Comparator.reverseOrder());
            default -> Comparator.comparingLong(
                    (AdminAiUsageUserRowDto r) -> r.summary().totalTokens()).reversed();
        };
        rows.sort(comparator);

        int totalElements = rows.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int from = Math.min(page * size, totalElements);
        int to = Math.min(from + size, totalElements);

        return new AdminAiUsageUsersPageDto(page, size, totalElements, totalPages, rows.subList(from, to));
    }

    /**
     * 单用户用量详情：个人资料、配额状态、汇总、每日趋势和按任务/模型/Provider 分解。
     */
    @Transactional(readOnly = true)
    public AdminAiUsageUserDetailDto userDetail(UUID userId, LocalDate startDate, LocalDate endDate) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        Instant start = toStartInstant(startDate);
        Instant end = toEndInstant(endDate);
        List<AiUsageLog> logs = usageRepository.findByUserIdAndCreatedAtBetweenOrderByCreatedAtAsc(userId, start, end);

        UsageAccumulator acc = new UsageAccumulator();
        Map<LocalDate, UsageAccumulator> dailyAccs = new TreeMap<>();
        Map<String, UsageAccumulator> taskAccs = new TreeMap<>();
        Map<String, UsageAccumulator> modelAccs = new TreeMap<>();
        Map<String, UsageAccumulator> providerAccs = new TreeMap<>();

        for (AiUsageLog log : logs) {
            acc.add(log);
            LocalDate day = LocalDateTime.ofInstant(log.getCreatedAt(), ZoneOffset.UTC).toLocalDate();
            dailyAccs.computeIfAbsent(day, k -> new UsageAccumulator()).add(log);
            taskAccs.computeIfAbsent(safeName(log.getTask()), k -> new UsageAccumulator()).add(log);
            modelAccs.computeIfAbsent(safeName(log.getModel()), k -> new UsageAccumulator()).add(log);
            providerAccs.computeIfAbsent(safeName(log.getProviderType()), k -> new UsageAccumulator()).add(log);
        }

        long currentMonthTokens = acc.getTotalTokens();
        Long quota = user.getMonthlyTokenQuota();
        Long remaining = quota != null ? Math.max(0, quota - currentMonthTokens) : null;
        boolean exceeded = quota != null && currentMonthTokens >= quota;

        return new AdminAiUsageUserDetailDto(
                user.getId().toString(), user.getUsername(), user.getEmail(), user.getRole(),
                user.getCreatedAt() != null ? user.getCreatedAt().toString() : null,
                quota, currentMonthTokens, remaining, exceeded,
                acc.toSummary(),
                dailyAccs.entrySet().stream().map(e -> e.getValue().toDaily(e.getKey().toString())).toList(),
                taskAccs.entrySet().stream().map(e -> e.getValue().toBreakdown(e.getKey())).toList(),
                modelAccs.entrySet().stream().map(e -> e.getValue().toBreakdown(e.getKey())).toList(),
                providerAccs.entrySet().stream().map(e -> e.getValue().toBreakdown(e.getKey())).toList());
    }

    /**
     * 统计当前月平台 AI token 用量已超过配额的用户数。
     */
    private long countQuotaExceededUsers() {
        List<User> usersWithQuota = userRepository.findByMonthlyTokenQuotaIsNotNull();
        if (usersWithQuota.isEmpty()) {
            return 0;
        }
        Instant monthStart = LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1)
                .atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant now = Instant.now();
        List<UUID> userIds = usersWithQuota.stream().map(User::getId).toList();

        List<Object[]> rows = usageRepository.sumTotalTokensByUserIdsAndProviderTypeAndCreatedAtBetween(
                userIds, "platformDefault", monthStart, now);
        Map<UUID, Long> usageMap = new HashMap<>();
        for (Object[] row : rows) {
            usageMap.put((UUID) row[0], (Long) row[1]);
        }

        long count = 0;
        for (User user : usersWithQuota) {
            long used = usageMap.getOrDefault(user.getId(), 0L);
            if (used >= user.getMonthlyTokenQuota()) {
                count++;
            }
        }
        return count;
    }

    private static String safeName(String name) {
        return (name == null || name.isBlank()) ? "unknown" : name;
    }

    private static Instant toStartInstant(LocalDate date) {
        return date != null ? date.atStartOfDay(ZoneOffset.UTC).toInstant() : Instant.EPOCH;
    }

    private static Instant toEndInstant(LocalDate date) {
        return date != null ? date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()
                : Instant.now().plusSeconds(1);
    }
}
