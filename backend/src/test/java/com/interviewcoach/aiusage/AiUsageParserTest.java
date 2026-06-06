package com.interviewcoach.aiusage;

import com.interviewcoach.aiusage.service.AiUsageMetadata;
import com.interviewcoach.aiusage.service.AiUsageParser;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiUsageParserTest {

    @Test
    void parsesChatCompletionsUsageIntoStandardTokenFields() {
        AiUsageMetadata usage = AiUsageParser.fromOpenAiResponse("""
                {
                  "usage": {
                    "prompt_tokens": 120,
                    "completion_tokens": 40,
                    "prompt_tokens_details": { "cached_tokens": 30 },
                    "completion_tokens_details": { "reasoning_tokens": 9 }
                  }
                }
                """).orElseThrow();

        assertThat(usage.inputTokens()).isEqualTo(120);
        assertThat(usage.outputTokens()).isEqualTo(40);
        assertThat(usage.cacheReadTokens()).isEqualTo(30);
        assertThat(usage.reasoningTokens()).isEqualTo(9);
        assertThat(usage.totalTokens()).isEqualTo(199);
        assertThat(usage.source()).isEqualTo("openAiResponseUsage");
        assertThat(usage.estimated()).isFalse();
    }

    @Test
    void parsesResponsesUsageNestedUnderResponse() {
        AiUsageMetadata usage = AiUsageParser.fromOpenAiResponse("""
                {
                  "type": "response.completed",
                  "response": {
                    "usage": {
                      "input_tokens": 80,
                      "output_tokens": 25,
                      "input_tokens_details": { "cached_tokens": 12 },
                      "output_tokens_details": { "reasoning_tokens": 6 }
                    }
                  }
                }
                """).orElseThrow();

        assertThat(usage.inputTokens()).isEqualTo(80);
        assertThat(usage.outputTokens()).isEqualTo(25);
        assertThat(usage.cacheReadTokens()).isEqualTo(12);
        assertThat(usage.reasoningTokens()).isEqualTo(6);
        assertThat(usage.totalTokens()).isEqualTo(123);
    }

    @Test
    void estimatesTokensWhenProviderUsageIsUnavailable() {
        AiUsageMetadata usage = AiUsageParser.estimate(
                "system prompt",
                "user prompt",
                "{\"ok\":true}",
                "estimatedFallback");

        assertThat(usage.totalTokens()).isGreaterThan(0);
        assertThat(usage.source()).isEqualTo("estimatedFallback");
        assertThat(usage.estimated()).isTrue();
    }
}
