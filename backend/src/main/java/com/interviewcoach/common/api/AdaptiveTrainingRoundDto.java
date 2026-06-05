package com.interviewcoach.common.api;

import java.util.List;

/**
 * 自适应专项训练——单轮记录，由 AI 评分和反馈，包含题目、用户回答、评分和反馈。
 *
 * @param roundIndex 轮次序号（从 0 开始）
 * @param question   本轮训练题目
 * @param answer     用户的回答内容
 * @param action     AI 决策的本轮动作（追问 / 换角度 / 达标停止等）
 * @param score      本轮评分（0-100）
 * @param feedback   AI 对本轮回答的反馈
 * @param problems   AI 识别到的回答问题列表
 */
public record AdaptiveTrainingRoundDto(
        int roundIndex,
        String question,
        String answer,
        String action,
        int score,
        String feedback,
        List<String> problems
) {
}
