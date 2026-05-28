package com.interviewcoach.training.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "training_sessions")
public class TrainingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private TrainingTask task;

    @Column(nullable = false)
    private String status = "in_progress";

    @Column(nullable = false)
    private int roundIndex = 0;

    @Column(nullable = false)
    private int minRounds = 2;

    @Column(nullable = false)
    private int maxRounds = 4;

    @Column(columnDefinition = "TEXT")
    private String currentQuestion;

    private String lastAction;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("roundIndex ASC")
    private List<TrainingSessionRound> rounds = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) { createdAt = now; }
        if (updatedAt == null) { updatedAt = now; }
    }

    @PreUpdate
    void preUpdate() { updatedAt = Instant.now(); }

    public UUID getId() { return id; }

    public TrainingTask getTask() { return task; }
    public void setTask(TrainingTask task) { this.task = task; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getRoundIndex() { return roundIndex; }
    public void setRoundIndex(int roundIndex) { this.roundIndex = roundIndex; }

    public int getMinRounds() { return minRounds; }
    public void setMinRounds(int minRounds) { this.minRounds = minRounds; }

    public int getMaxRounds() { return maxRounds; }
    public void setMaxRounds(int maxRounds) { this.maxRounds = maxRounds; }

    public String getCurrentQuestion() { return currentQuestion; }
    public void setCurrentQuestion(String currentQuestion) { this.currentQuestion = currentQuestion; }

    public String getLastAction() { return lastAction; }
    public void setLastAction(String lastAction) { this.lastAction = lastAction; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public List<TrainingSessionRound> getRounds() { return rounds; }
    public void setRounds(List<TrainingSessionRound> rounds) { this.rounds = rounds; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
