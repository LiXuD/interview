package com.interviewcoach.common.api;

import java.util.List;

/**
 * AI 生成的候选人画像摘要草稿 DTO，原文不落库，仅用于前端展示供用户编辑确认。
 *
 * @param summary       AI 生成的经历摘要
 * @param skills        AI 提取的技能列表
 * @param projects      AI 提取的项目经历列表
 * @param experience    AI 提取的工作经历列表
 * @param rawTextLength 原始简历文本字符长度（用于前端校验）
 */
public record CandidateProfileDraftDto(String summary, List<String> skills, List<String> projects, List<String> experience, int rawTextLength) {
}
