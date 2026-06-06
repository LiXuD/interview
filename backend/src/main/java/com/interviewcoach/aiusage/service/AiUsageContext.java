package com.interviewcoach.aiusage.service;

/**
 * AI 客户端到网关之间传递单次调用 usage 的轻量上下文。
 */
public final class AiUsageContext {

    private static final ThreadLocal<AiUsageMetadata> USAGE = new ThreadLocal<>();

    private AiUsageContext() {
    }

    public static void setUsage(AiUsageMetadata usage) {
        if (usage == null) {
            USAGE.remove();
        } else {
            USAGE.set(usage);
        }
    }

    public static AiUsageMetadata currentUsage() {
        return USAGE.get();
    }

    public static void clear() {
        USAGE.remove();
    }
}
