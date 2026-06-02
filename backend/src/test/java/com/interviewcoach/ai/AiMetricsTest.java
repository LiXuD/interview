package com.interviewcoach.ai;

import com.interviewcoach.ai.service.AiMetrics;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class AiMetricsTest {

    private MeterRegistry registry;
    private AiMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new AiMetrics(registry);
    }

    @Test
    void recordCallCreatesTimerAndCounter() {
        long start = metrics.startTimerNanos();

        metrics.recordCall(start, "jobBrief", "platformDefault", "gpt-4", "chatCompletions", "success");

        Timer timer = registry.find("ai.call.duration")
                .tag("task", "jobBrief")
                .tag("provider", "platformDefault")
                .tag("model", "gpt-4")
                .tag("mode", "chatCompletions")
                .tag("outcome", "success")
                .timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
        assertThat(timer.totalTime(TimeUnit.NANOSECONDS)).isGreaterThan(0);

        Counter counter = registry.find("ai.call.total")
                .tag("task", "jobBrief")
                .tag("provider", "platformDefault")
                .tag("model", "gpt-4")
                .tag("mode", "chatCompletions")
                .tag("outcome", "success")
                .counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1);
    }

    @Test
    void recordCallFailureTracksDifferentOutcome() {
        long start = metrics.startTimerNanos();

        metrics.recordCall(start, "trainingFeedback", "userOpenAICompatible", "gpt-user", "responses", "failure");

        Counter counter = registry.find("ai.call.total")
                .tag("task", "trainingFeedback")
                .tag("outcome", "failure")
                .counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1);
    }

    @Test
    void recordParseFailureIncrementsCounter() {
        metrics.recordParseFailure("assessmentResult");

        Counter counter = registry.find("ai.parse.failure")
                .tag("task", "assessmentResult")
                .counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1);
    }

    @Test
    void recordValidationFailureIncrementsCounter() {
        metrics.recordValidationFailure("mockInterviewReport");

        Counter counter = registry.find("ai.validation.failure")
                .tag("task", "mockInterviewReport")
                .counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1);
    }

    @Test
    void safeTagHandlesNullAndBlank() {
        long start = metrics.startTimerNanos();

        metrics.recordCall(start, null, "", "model", null, "success");

        Counter counter = registry.find("ai.call.total")
                .tag("task", "unknown")
                .tag("provider", "unknown")
                .tag("mode", "unknown")
                .counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1);
    }

    @Test
    void multipleCallsAccumulateMetrics() {
        long start1 = metrics.startTimerNanos();
        metrics.recordCall(start1, "jobBrief", "platformDefault", "gpt-4", "chatCompletions", "success");

        long start2 = metrics.startTimerNanos();
        metrics.recordCall(start2, "jobBrief", "platformDefault", "gpt-4", "chatCompletions", "success");

        long start3 = metrics.startTimerNanos();
        metrics.recordCall(start3, "jobBrief", "platformDefault", "gpt-4", "chatCompletions", "failure");

        Counter successCounter = registry.find("ai.call.total")
                .tag("task", "jobBrief")
                .tag("outcome", "success")
                .counter();
        assertThat(successCounter.count()).isEqualTo(2);

        Counter failureCounter = registry.find("ai.call.total")
                .tag("task", "jobBrief")
                .tag("outcome", "failure")
                .counter();
        assertThat(failureCounter.count()).isEqualTo(1);
    }
}
