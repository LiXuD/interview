package com.interviewcoach.common.error;

/**
 * AI 返回的结构化输出解析失败时抛出，对应统一错误码 AI_PARSE_FAILED。
 */
public class AiParseException extends RuntimeException {
    /**
     * 创建 AI 结构化输出解析失败的异常实例。
     *
     * @param task 发生解析失败的 AI 任务标识
     */
    public AiParseException(String task) {
        super("AI returned invalid structured output for task: " + task);
    }
}
