package com.interviewcoach.jobbrief.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class JobBriefSkill {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String importance;

    @Column(nullable = false)
    private String userLevel;

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
