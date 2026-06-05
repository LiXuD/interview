package com.interviewcoach.common.api;

import java.util.List;

/**
 * 测评逐题评分 DTO，由 AI 对单题回答进行评分和诊断，包含得分、结构诊断和改进建议。
 *
 * @param questionIndex    题目序号（从 0 开始）
 * @param score            本题评分（0-100）
 * @param dimension        本题所属能力维度
 * @param feedback         AI 对本题的反馈
 * @param problems         AI 识别到的回答问题列表
 * @param improvedExample  改进后的示范回答
 * @param answerStructure  回答结构诊断（STAR+ 框架拆解）
 * @param followUpRisks    追问风险点列表
 * @param contentHighlights 回答中的亮点
 * @param contentGaps      回答中的缺失点
 */
public record AssessmentQuestionScoreDto(
        int questionIndex,
        int score,
        String dimension,
        String feedback,
        List<String> problems,
        String improvedExample,
        AnswerStructureDto answerStructure,
        List<String> followUpRisks,
        List<String> contentHighlights,
        List<String> contentGaps
) {
}
