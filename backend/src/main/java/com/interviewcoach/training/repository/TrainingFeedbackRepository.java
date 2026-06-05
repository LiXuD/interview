package com.interviewcoach.training.repository;

import com.interviewcoach.training.entity.TrainingFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * 训练反馈数据访问接口。
 */
public interface TrainingFeedbackRepository extends JpaRepository<TrainingFeedback, UUID> {
}
