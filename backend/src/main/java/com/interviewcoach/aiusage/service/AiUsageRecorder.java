package com.interviewcoach.aiusage.service;

/**
 * AI usage 记录入口。
 */
public interface AiUsageRecorder {

    void record(AiUsageLogCommand command);

    static AiUsageRecorder noop() {
        return command -> {
        };
    }
}
