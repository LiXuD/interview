package com.interviewcoach.ai.repository;

import com.interviewcoach.ai.entity.AiProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AiProviderRepository extends JpaRepository<AiProvider, UUID> {
    List<AiProvider> findByUserIdOrderByCreatedAtDesc(UUID userId);
    Optional<AiProvider> findByIdAndUserId(UUID id, UUID userId);
    Optional<AiProvider> findByUserIdAndIsDefaultTrue(UUID userId);
    boolean existsByUserId(UUID userId);
    void deleteByUserId(UUID userId);
}
