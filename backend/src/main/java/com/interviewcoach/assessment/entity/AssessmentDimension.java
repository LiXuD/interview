package com.interviewcoach.assessment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * 测评维度可嵌入对象，记录单个能力维度的评分和理由。
 */
@Embeddable
public class AssessmentDimension {

    /** 维度名称，如 technicalDepth、systemThinking 等 */
    @Column(nullable = false)
    private String name;

    /** 维度评分，范围 0-100 */
    @Column(nullable = false)
    private int score;

    /** 评分理由 */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    public AssessmentDimension() {
    }

    public AssessmentDimension(String name, int score, String reason) {
        this.name = name;
        this.score = score;
        this.reason = reason;
    }

    public String getName() { return name; }
    public int getScore() { return score; }
    public String getReason() { return reason; }
}
