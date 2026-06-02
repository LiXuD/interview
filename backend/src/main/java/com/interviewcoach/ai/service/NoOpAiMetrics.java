package com.interviewcoach.ai.service;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

public class NoOpAiMetrics extends AiMetrics {

    public NoOpAiMetrics() {
        super(new SimpleMeterRegistry());
    }

    @Override
    public long startTimerNanos() {
        return 0L;
    }

    @Override
    public void recordCall(long startNanos, String task, String provider, String model,
                           String mode, String outcome) {
        // no-op for tests that don't need metrics
    }

    @Override
    public void recordParseFailure(String task) {
        // no-op
    }

    @Override
    public void recordValidationFailure(String task) {
        // no-op
    }
}
