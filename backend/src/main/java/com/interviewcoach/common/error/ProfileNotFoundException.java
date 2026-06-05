package com.interviewcoach.common.error;

import java.util.UUID;

/**
 * 根据 ID 查找候选人画像未找到时抛出。
 */
public class ProfileNotFoundException extends RuntimeException {
    /**
     * 创建指定 ID 的候选人画像未找到的异常实例。
     *
     * @param profileId 候选人画像 ID 或关联的目标岗位 ID
     */
    public ProfileNotFoundException(UUID profileId) {
        super("Profile not found: " + profileId);
    }
}
