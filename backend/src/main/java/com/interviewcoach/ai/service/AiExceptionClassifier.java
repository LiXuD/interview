package com.interviewcoach.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * AI 异常分类工具。用于判断异常链中是否包含 JSON 解析错误，
 * 以便区分结构化输出映射失败和其他调用异常。
 */
final class AiExceptionClassifier {

    private AiExceptionClassifier() {
    }

    /**
     * 判断异常链中是否存在 {@link JsonProcessingException}。
     *
     * @param ex 待检查的异常
     * @return 如果异常链中包含 JSON 解析异常则返回 true
     */
    static boolean hasJsonProcessingCause(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof JsonProcessingException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
