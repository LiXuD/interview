package com.interviewcoach.profile.repository;

import com.interviewcoach.profile.entity.CandidateProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CandidateProfileRepository extends JpaRepository<CandidateProfile, UUID> {
    Optional<CandidateProfile> findByTargetIdAndUserId(UUID targetId, UUID userId);
    List<CandidateProfile> findByUserIdOrderByConfirmedAtDesc(UUID userId);
}
