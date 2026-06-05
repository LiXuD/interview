package com.interviewcoach.common.api;

import java.util.List;

/**
 * 教练记忆导入请求，用于将本机记忆摘要导入到指定岗位目标。
 *
 * @param targetId  目标岗位 ID
 * @param summaries 待导入的记忆摘要列表
 */
public record CoachingMemoryImportRequest(
        String targetId,
        List<String> summaries
) {
}
