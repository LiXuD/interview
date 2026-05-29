package com.interviewcoach.ai;

import com.interviewcoach.ai.entity.AiProvider;
import com.interviewcoach.ai.service.AiPrompt;
import com.interviewcoach.ai.service.PlatformAiProperties;
import com.interviewcoach.ai.service.SpringAiCallContext;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SpringAiCallContextTest {

    @Test
    void userContextContainsOnlyLowRiskMetadata() {
        AiProvider provider = new AiProvider();
        UUID providerId = UUID.randomUUID();
        ReflectionTestUtils.setField(provider, "id", providerId);
        provider.setModel("gpt-user");
        provider.setOpenaiApiMode("chatCompletions");

        var context = SpringAiCallContext.user(
                new AiPrompt(
                        AiPrompt.TASK_JOB_BRIEF,
                        "target-1",
                        "secret system prompt",
                        "resume raw text"),
                provider,
                "request-1");

        assertThat(context)
                .containsEntry("ai.task", AiPrompt.TASK_JOB_BRIEF)
                .containsEntry("ai.provider", "userOpenAICompatible")
                .containsEntry("ai.providerId", providerId.toString())
                .containsEntry("ai.model", "gpt-user")
                .containsEntry("ai.mode", "chatCompletions")
                .containsEntry("ai.targetId", "target-1")
                .containsEntry("ai.requestId", "request-1");
        assertThat(context.toString())
                .doesNotContain("secret system prompt")
                .doesNotContain("resume raw text")
                .doesNotContain("apiKey");
    }

    @Test
    void platformContextContainsOnlyLowRiskMetadata() {
        PlatformAiProperties properties = new PlatformAiProperties();
        properties.setModel("gpt-platform");
        properties.setMode("chatCompletions");
        properties.setApiKey("sk-platform-secret");

        var context = SpringAiCallContext.platform(
                new AiPrompt(
                        AiPrompt.TASK_TRAINING_FEEDBACK,
                        "task-1",
                        "secret system prompt",
                        "answer raw text"),
                properties,
                "request-2");

        assertThat(context)
                .containsEntry("ai.task", AiPrompt.TASK_TRAINING_FEEDBACK)
                .containsEntry("ai.provider", "platformDefault")
                .containsEntry("ai.model", "gpt-platform")
                .containsEntry("ai.mode", "chatCompletions")
                .containsEntry("ai.targetId", "task-1")
                .containsEntry("ai.requestId", "request-2");
        assertThat(context.toString())
                .doesNotContain("secret system prompt")
                .doesNotContain("answer raw text")
                .doesNotContain("sk-platform-secret");
    }
}
