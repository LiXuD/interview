package com.interviewcoach.user.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * 用户实体，存储用户基本信息和认证标识。
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String username;

    /** Sign in with Apple 的唯一用户标识，仅 Apple 登录用户有值。 */
    @Column(unique = true)
    private String appleUserId;

    @Column
    private String email;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getAppleUserId() {
        return appleUserId;
    }

    public void setAppleUserId(String appleUserId) {
        this.appleUserId = appleUserId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
