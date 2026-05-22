package com.interviewcoach.assessment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class AssessmentDimension {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int score;

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
