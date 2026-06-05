package com.interviewcoach.mockinterview.repository;

import com.interviewcoach.mockinterview.entity.MockInterviewMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * 模拟面试消息数据访问接口。
 */
public interface MockInterviewMessageRepository extends JpaRepository<MockInterviewMessage, UUID> {
}
