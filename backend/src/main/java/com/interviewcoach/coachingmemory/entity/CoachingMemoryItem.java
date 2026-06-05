package com.interviewcoach.coachingmemory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * 教练记忆条目可嵌入对象，包含内容、来源和可信度。
 * source 取值：confirmed / observed / corrected / inferred / rejected。
 * confidence 取值：high / medium / low。
 */
@Embeddable
public class CoachingMemoryItem {

    /** 记忆内容描述 */
    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    /** 来源类型：confirmed / observed / corrected / inferred / rejected */
    @Column(name = "source", nullable = false)
    private String source;

    /** 可信度：high / medium / low */
    @Column(name = "confidence", nullable = false)
    private String confidence;

    protected CoachingMemoryItem() {
    }

    public CoachingMemoryItem(String content, String source, String confidence) {
        this.content = content;
        this.source = source;
        this.confidence = confidence;
    }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getConfidence() { return confidence; }
    public void setConfidence(String confidence) { this.confidence = confidence; }
}
