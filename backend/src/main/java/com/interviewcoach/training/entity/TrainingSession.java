package com.interviewcoach.training.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 自适应训练会话实体，围绕单个训练短板进行 2-4 轮 AI 追问训练。
 * 每轮由 AI 动态决定继续追问、换角度、达标或停止。
 */
@Entity
@Table(name = "training_sessions")
public class TrainingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private TrainingTask task;

    /** 状态：in_progress / completed */
    @Column(nullable = false)
    private String status = "in_progress";

    /** 当前已完成的回答轮数 */
    @Column(nullable = false)
    private int roundIndex = 0;

    /** 最少训练轮数，未达到前 AI 不会返回 pass/stop */
    @Column(nullable = false)
    private int minRounds = 2;

    /** 最多训练轮数，达到后自动结束会话 */
    @Column(nullable = false)
    private int maxRounds = 4;

    /** AI 当前提出的问题 */
    @Column(columnDefinition = "TEXT")
    private String currentQuestion;

    /** 上一轮 AI 决定的动作：continue / pass / switch / stop */
    private String lastAction;

    /** 训练结束后的总结 */
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
