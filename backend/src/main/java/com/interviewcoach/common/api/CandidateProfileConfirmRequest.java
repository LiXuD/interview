package com.interviewcoach.common.api;

import java.util.List;

/**
 * 确认候选人画像请求，用户编辑并确认 AI 生成的摘要后提交。
 *
 * @param targetId   关联的岗位目标 ID
 * @param summary    用户确认后的经历摘要
 * @param skills     技能列表
 * @param projects   项目经历列表
 * @param experience 工作经历列表
 */
public record CandidateProfileConfirmRequest(String targetId, String summary, List<String> skills, List<String> projects, List<String> experience) {
}
