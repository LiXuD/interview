package com.interviewcoach.mockinterview.repository;

import com.interviewcoach.mockinterview.entity.MockInterviewMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MockInterviewMessageRepository extends JpaRepository<MockInterviewMessage, UUID> {
}
