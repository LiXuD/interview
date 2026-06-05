package com.interviewcoach.common.api;

/**
 * 生成候选人画像摘要草稿请求，简历原文临时上传到后端内存用于 AI 摘要，不落库。
 *
 * @param resumeText     简历原文（仅在内存中使用，不记录日志、不持久化）
 * @param projectRawText 项目经历原文（仅在内存中使用，不记录日志、不持久化）
 */
public record CandidateProfileDraftRequest(String resumeText, String projectRawText) {
}
