package com.interviewcoach.acceptance;

import com.interviewcoach.ai.service.*;
import com.interviewcoach.common.api.JobBriefDto;
import com.interviewcoach.common.error.AiParseException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Task 26/27: AI 可靠性测试 — 验证 retry、parse failure metrics，不依赖真实模型。
 *
 * 使用模拟瞬时失败和解析失败场景验证指标记录。
 */
@SpringBootTest
@ActiveProfiles("test")
class AiReliabilityTest {

    @TestConfiguration
    static class RetryTestConfig {
        private static final AtomicInteger generateJsonCalls = new AtomicInteger(0);
        private static volatile boolean returnInvalidJson;

        @Bean
        @Primary
        public PlatformAiClient retrySimulatingClient() {
            return new PlatformAiClient() {
                @Override
                public String generateJson(AiPrompt prompt) {
                    if (returnInvalidJson) {
                        generateJsonCalls.incrementAndGet();
                        return "not-json";
                    }
                    int call = generateJsonCalls.incrementAndGet();
                    if (call == 1) {
                        throw new org.springframework.web.client.HttpServerErrorException(
                                org.springframework.http.HttpStatusCode.valueOf(502),
                                "Bad Gateway",
                                "{}".getBytes(java.nio.charset.StandardCharsets.UTF_8),
                                java.nio.charset.StandardCharsets.UTF_8);
                    }
                    return """
                            {
                              "targetId": "t1",
                              "roleSummary": "Backend engineer",
                              "skillMap": [{"name": "Java", "importance": "required", "userLevel": "solid", "gap": "depth"}],
                              "mustHaveSkills": ["Java"],
                              "niceToHaveSkills": [],
                              "businessContext": [],
                              "interviewTopics": [],
                              "candidateMatch": [],
                              "riskAreas": [],
                              "confidence": 0.7
                            }
                            """;
                }

                @Override
                public <T> T generateEntity(AiPrompt prompt, Class<T> responseType) {
                    return null;
                }
            };
        }

        static void resetCallCount() {
            generateJsonCalls.set(0);
            returnInvalidJson = false;
        }

        static void useInvalidJson() {
            returnInvalidJson = true;
        }
    }

    @Autowired
    private AiStructuredOutputService aiService;

    @Autowired
    private MeterRegistry meterRegistry;

    @BeforeEach
    void clearMetrics() {
        meterRegistry.clear();
        RetryTestConfig.resetCallCount();
    }

    @Test
    @DisplayName("retry 指标应在 502 瞬时失败重试时记录")
    void retryMetricRecordedOnTransientFailure() {
        AiPrompt prompt = new AiPrompt(
                AiPrompt.TASK_JOB_BRIEF, "test-target",
                "你是面试教练", "生成岗位画像");
        JobBriefDto result = aiService.generateJobBrief(prompt);
        assertThat(result).isNotNull();
        assertThat(result.roleSummary()).isEqualTo("Backend engineer");

        // Spring structured output 路径 (generateEntity) 返回 null 后走 raw JSON 路径
        // raw JSON 路径第一次遇到 502，重试成功，所以 retry=1, call=1 (只记录成功调用)
        Counter retryCounter = meterRegistry.find("ai.call.retry").counter();
        assertThat(retryCounter).isNotNull();
        assertThat(retryCounter.count()).isEqualTo(1);

        Counter callCounter = meterRegistry.find("ai.call.total").counter();
        assertThat(callCounter).isNotNull();
        assertThat(callCounter.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("parse failure 应记录 ai.parse.failure 指标")
    void parseFailureRecordsMetric() {
        RetryTestConfig.useInvalidJson();
        AiPrompt prompt = new AiPrompt(
                AiPrompt.TASK_JOB_BRIEF, "test-target-parse",
                "你是面试教练", "生成岗位画像");
        assertThatThrownBy(() -> aiService.generateJobBrief(prompt))
                .isInstanceOf(AiParseException.class);

        Counter parseCounter = meterRegistry.find("ai.parse.failure").counter();
        assertThat(parseCounter).isNotNull();
        assertThat(parseCounter.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("正常 AI 调用应记录 ai.call.total 指标")
    void normalCallRecordsMetric() {
        AiPrompt prompt = new AiPrompt(
                AiPrompt.TASK_JOB_BRIEF, "test-target-call",
                "你是面试教练", "生成岗位画像");
        aiService.generateJobBrief(prompt);

        Counter callCounter = meterRegistry.find("ai.call.total").counter();
        assertThat(callCounter).isNotNull();
        assertThat(callCounter.count()).isGreaterThan(0);
    }

    @Test
    @DisplayName("AiParseException 包含 task 信息")
    void aiParseExceptionContainsTaskInfo() {
        AiParseException ex = new AiParseException("testTask");
        assertThat(ex).hasMessageContaining("testTask");
    }
}
