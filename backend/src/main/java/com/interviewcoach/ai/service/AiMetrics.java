package com.interviewcoach.ai.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * AI 调用指标采集组件。通过 Micrometer 记录 AI 调用耗时、成功/失败计数、
 * 解析失败、校验失败、超时、重试和估算 token 用量等可观测数据。
 * <p>仅采集低风险元数据（task、provider、model、mode、latency 等），
 * 禁止采集 prompt、completion、简历原文或 API Key。</p>
 */
@Component
public class AiMetrics {

    private final MeterRegistry registry;

    public AiMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /** 记录调用开始时间戳（纳秒） */
    public long startTimerNanos() {
        return System.nanoTime();
    }

    /**
     * 记录一次 AI 调用的耗时和次数
     *
     * @param startNanos 调用开始时间戳
     * @param task       AI 任务类型
     * @param provider   Provider 标识
     * @param model      模型名称
     * @param mode       API 模式
     * @param outcome    结果：success 或 failure
     */
    public void recordCall(long startNanos, String task, String provider, String model,
                           String mode, String outcome) {
        long durationNanos = System.nanoTime() - startNanos;

        Timer.builder("ai.call.duration")
                .description("AI call latency")
                .tag("task", safeTag(task))
                .tag("provider", safeTag(provider))
                .tag("model", safeTag(model))
                .tag("mode", safeTag(mode))
                .tag("outcome", outcome)
                .register(registry)
                .record(durationNanos, java.util.concurrent.TimeUnit.NANOSECONDS);

        Counter.builder("ai.call.total")
                .description("Total AI calls")
                .tag("task", safeTag(task))
                .tag("provider", safeTag(provider))
                .tag("model", safeTag(model))
                .tag("mode", safeTag(mode))
                .tag("outcome", outcome)
                .register(registry)
                .increment();
    }

    /**
     * 记录 AI 结构化输出解析失败（不含 provider 维度）
     *
     * @param task AI 任务类型标识
     */
    public void recordParseFailure(String task) {
        Counter.builder("ai.parse.failure")
                .description("AI structured output parse failures")
                .tag("task", safeTag(task))
                .register(registry)
                .increment();
    }

    /**
     * 记录 AI 结构化输出解析失败（含 provider 维度）
     *
     * @param task     AI 任务类型标识
     * @param provider Provider 标识
     * @param model    模型名称
     * @param mode     API 模式
     */
    public void recordParseFailure(String task, String provider, String model, String mode) {
        Counter.builder("ai.parse.failure")
                .description("AI structured output parse failures")
                .tag("task", safeTag(task))
                .tag("provider", safeTag(provider))
                .tag("model", safeTag(model))
                .tag("mode", safeTag(mode))
                .register(registry)
                .increment();
    }

    /**
     * 记录 AI 输出业务校验失败（不含 provider 维度）
     *
     * @param task AI 任务类型标识
     */
    public void recordValidationFailure(String task) {
        Counter.builder("ai.validation.failure")
                .description("AI output validation failures")
                .tag("task", safeTag(task))
                .register(registry)
                .increment();
    }

    /**
     * 记录 AI 输出业务校验失败（含 provider 维度）
     *
     * @param task     AI 任务类型标识
     * @param provider Provider 标识
     * @param model    模型名称
     * @param mode     API 模式
     */
    public void recordValidationFailure(String task, String provider, String model, String mode) {
        Counter.builder("ai.validation.failure")
                .description("AI output validation failures")
                .tag("task", safeTag(task))
                .tag("provider", safeTag(provider))
                .tag("model", safeTag(model))
                .tag("mode", safeTag(mode))
                .register(registry)
                .increment();
    }

    /**
     * 记录 AI 调用超时
     *
     * @param task     AI 任务类型标识
     * @param provider Provider 标识
     * @param model    模型名称
     * @param mode     API 模式
     */
    public void recordTimeout(String task, String provider, String model, String mode) {
        Counter.builder("ai.timeout")
                .description("AI call timeouts")
                .tag("task", safeTag(task))
                .tag("provider", safeTag(provider))
                .tag("model", safeTag(model))
                .tag("mode", safeTag(mode))
                .register(registry)
                .increment();
    }

    /**
     * 记录 AI 调用瞬时异常重试
     *
     * @param task     AI 任务类型标识
     * @param provider Provider 标识
     * @param model    模型名称
     * @param mode     API 模式
     */
    public void recordRetry(String task, String provider, String model, String mode) {
        Counter.builder("ai.call.retry")
                .description("AI call transient retries")
                .tag("task", safeTag(task))
                .tag("provider", safeTag(provider))
                .tag("model", safeTag(model))
                .tag("mode", safeTag(mode))
                .register(registry)
                .increment();
    }

    /**
     * 记录估算的 token 用量
     *
     * @param task            AI 任务类型标识
     * @param provider        Provider 标识
     * @param model           模型名称
     * @param estimatedTokens 估算的 token 数量
     */
    public void recordTokenUsage(String task, String provider, String model, int estimatedTokens) {
        Counter.builder("ai.token.usage")
                .description("Estimated AI token usage")
                .tag("task", safeTag(task))
                .tag("provider", safeTag(provider))
                .tag("model", safeTag(model))
                .register(registry)
                .increment(estimatedTokens);
    }

    /** 获取底层 Micrometer MeterRegistry */
    public MeterRegistry meterRegistry() {
        return registry;
    }

    private static String safeTag(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }
}
