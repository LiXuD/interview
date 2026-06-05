package com.interviewcoach.common.api;

/**
 * 回答结构诊断 DTO，由 AI 按 STAR+ 框架拆解用户回答的各组成部分。
 *
 * @param background 回答中的背景描述
 * @param task       回答中的任务/目标描述
 * @param action     回答中的具体行动描述
 * @param result     回答中的结果描述
 * @param tradeoff   回答中的权衡取舍描述
 * @param review     回答中的复盘反思描述
 */
public record AnswerStructureDto(
        String background,
        String task,
        String action,
        String result,
        String tradeoff,
        String review
) {
}
