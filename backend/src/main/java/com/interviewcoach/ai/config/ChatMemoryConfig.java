package com.interviewcoach.ai.config;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatMemoryConfig {

    /**
     * Configures a MessageWindowChatMemory with a window size of 12 messages (6 turns).
     * This is used for mock interview context management.
     *
     * Constraints:
     * - Window size = 12 messages (equivalent to 6 turns of user+assistant)
     * - ChatMemory does NOT store business logic (confirmed/rejected facts)
     * - Business long-term memory is stored in CoachingMemory entity
     * - Resume text, API keys, and AI chain-of-thought are NEVER stored
     */
    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .maxMessages(12)
                .build();
    }
}
