package com.interviewcoach.ai.config;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI 聊天记忆配置。为模拟面试提供滑动窗口短上下文管理。
 */
@Configuration
public class ChatMemoryConfig {

    /**
     * 配置消息窗口聊天记忆，窗口大小 12 条消息（6 轮对话）。
     * <p>约束：仅用于模拟面试短窗口上下文，不存储业务教练记忆；简历原文、API Key 和 AI 思维链永不存储。</p>
     */
    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .maxMessages(12)
                .build();
    }
}
