package com.interviewcoach.agent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Agent 运行时配置属性。绑定 {@code app.agent} 前缀，
 * 控制教练 Agent 事件分发和线程池行为。
 */
@ConfigurationProperties(prefix = "app.agent")
public class AgentRuntimeProperties {

    /** 是否启用事件分发 */
    private boolean dispatchEnabled = true;

    /** 是否异步执行 Agent 事件 */
    private boolean asyncEnabled = true;

    /** 线程池核心线程数 */
    private int executorCorePoolSize = 1;

    /** 线程池最大线程数 */
    private int executorMaxPoolSize = 1;

    /** 线程池队列容量 */
    private int executorQueueCapacity = 100;

    public boolean isDispatchEnabled() {
        return dispatchEnabled;
    }

    public void setDispatchEnabled(boolean dispatchEnabled) {
        this.dispatchEnabled = dispatchEnabled;
    }

    public boolean isAsyncEnabled() {
        return asyncEnabled;
    }

    public void setAsyncEnabled(boolean asyncEnabled) {
        this.asyncEnabled = asyncEnabled;
    }

    public int getExecutorCorePoolSize() {
        return executorCorePoolSize;
    }

    public void setExecutorCorePoolSize(int executorCorePoolSize) {
        this.executorCorePoolSize = executorCorePoolSize;
    }

    public int getExecutorMaxPoolSize() {
        return executorMaxPoolSize;
    }

    public void setExecutorMaxPoolSize(int executorMaxPoolSize) {
        this.executorMaxPoolSize = executorMaxPoolSize;
    }

    public int getExecutorQueueCapacity() {
        return executorQueueCapacity;
    }

    public void setExecutorQueueCapacity(int executorQueueCapacity) {
        this.executorQueueCapacity = executorQueueCapacity;
    }
}
