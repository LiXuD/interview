package com.interviewcoach.ai.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class AiMetrics {

    private final MeterRegistry registry;

    public AiMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public long startTimerNanos() {
        return System.nanoTime();
    }

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

    public void recordParseFailure(String task) {
        Counter.builder("ai.parse.failure")
                .description("AI structured output parse failures")
                .tag("task", safeTag(task))
                .register(registry)
                .increment();
    }

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

    public void recordValidationFailure(String task) {
        Counter.builder("ai.validation.failure")
                .description("AI output validation failures")
                .tag("task", safeTag(task))
                .register(registry)
                .increment();
    }

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

    public void recordTokenUsage(String task, String provider, String model, int estimatedTokens) {
        Counter.builder("ai.token.usage")
                .description("Estimated AI token usage")
                .tag("task", safeTag(task))
                .tag("provider", safeTag(provider))
                .tag("model", safeTag(model))
                .register(registry)
                .increment(estimatedTokens);
    }

    private static String safeTag(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }
}
