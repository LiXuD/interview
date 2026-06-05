package com.interviewcoach.common.api;

import java.util.List;

/**
 * 已确认的候选人画像 DTO，用户确认摘要后持久化到远端。
 *
 * @param id          画像 ID
 * @param targetId    关联的岗位目标 ID
 * @param summary     用户确认后的经历摘要
 * @param skills      技能列表
 * @param projects    项目经历列表
 * @param experience  工作经历列表
 * @param confirmedAt 确认时间
 */
public record CandidateProfileDto(String id, String targetId, String summary, List<String> skills, List<String> projects, List<String> experience, String confirmedAt) {
}
