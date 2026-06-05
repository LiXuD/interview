package com.interviewcoach.agent.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Agent 运行时配置。注册教练 Agent 事件处理的线程池 Bean。
 */
@Configuration
@EnableConfigurationProperties(AgentRuntimeProperties.class)
public class AgentRuntimeConfig {

    /**
     * 创建教练 Agent 事件处理线程池。
     * 线程名前缀为 "coach-agent-"，用于日志追踪。
     */
    @Bean
    public Executor coachAgentExecutor(AgentRuntimeProperties properties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("coach-agent-");
        executor.setCorePoolSize(properties.getExecutorCorePoolSize());
        executor.setMaxPoolSize(properties.getExecutorMaxPoolSize());
        executor.setQueueCapacity(properties.getExecutorQueueCapacity());
        executor.initialize();
        return executor;
    }
}
