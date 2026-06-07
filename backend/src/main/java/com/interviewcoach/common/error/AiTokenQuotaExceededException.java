package com.interviewcoach.common.error;

/**
 * AI Token 月度配额超限异常，当用户平台 AI 用量超过配额时抛出。
 */
public class AiTokenQuotaExceededException extends RuntimeException {

    public AiTokenQuotaExceededException(String message) {
        super(message);
    }
}
