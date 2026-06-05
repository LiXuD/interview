package com.interviewcoach.jobbrief.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * 岗位画像技能可嵌入对象，描述单个技能的重要性、候选人水平和差距。
 */
@Embeddable
public class JobBriefSkill {

    /** 技能名称 */
    @Column(nullable = false)
    private String name;

    /** 重要程度：required / important / bonus */
    @Column(nullable = false)
    private String importance;

    /** 候选人当前水平：unknown / weak / basic / solid / strong */
    @Column(nullable = false)
    private String userLevel;

    /** 候选人需要补充的能力描述 */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String gap;

    public JobBriefSkill() {
    }

    public JobBriefSkill(String name, String importance, String userLevel, String gap) {
        this.name = name;
        this.importance = importance;
        this.userLevel = userLevel;
        this.gap = gap;
    }

    public String getName() { return name; }

    public String getImportance() { return importance; }

    public String getUserLevel() { return userLevel; }

    public String getGap() { return gap; }
}
