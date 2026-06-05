package com.interviewcoach.common.api;

/**
 * 岗位画像生成请求，指定要分析的岗位目标。
 *
 * @param targetId 岗位目标 ID
 */
public record JobBriefGenerateRequest(
        String targetId
) {
}
