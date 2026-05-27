package com.interviewcoach.coachingmemory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class CoachingMemoryItem {

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "source", nullable = false)
    private String source;

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
