package com.interviewcoach.target.service;

import com.interviewcoach.common.api.InterviewTargetCreateRequest;
import com.interviewcoach.common.api.InterviewTargetDto;
import com.interviewcoach.common.api.InterviewTargetUpdateRequest;
import com.interviewcoach.common.error.TargetNotFoundException;
import com.interviewcoach.target.entity.InterviewTarget;
import com.interviewcoach.target.repository.InterviewTargetRepository;
import com.interviewcoach.user.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class InterviewTargetService {

    private static final Set<String> VALID_STATUSES = Set.of("active", "completed", "archived");

    private final InterviewTargetRepository targetRepository;

    public InterviewTargetService(InterviewTargetRepository targetRepository) {
        this.targetRepository = targetRepository;
    }

    @Transactional
    public InterviewTargetDto createTarget(User user, InterviewTargetCreateRequest request) {
        InterviewTarget target = new InterviewTarget();
        target.setUser(user);
        target.setTitle(request.title());
        target.setJd(request.jd());
        target = targetRepository.save(target);
        return toDto(target);
    }

    @Transactional(readOnly = true)
    public List<InterviewTargetDto> listTargets(UUID userId) {
        return targetRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public InterviewTargetDto getTarget(UUID targetId, UUID userId) {
        InterviewTarget target = targetRepository.findByIdAndUserId(targetId, userId)
                .orElseThrow(() -> new TargetNotFoundException(targetId));
        return toDto(target);
    }

    @Transactional
    public InterviewTargetDto updateTarget(UUID targetId, UUID userId, InterviewTargetUpdateRequest request) {
        InterviewTarget target = targetRepository.findByIdAndUserId(targetId, userId)
                .orElseThrow(() -> new TargetNotFoundException(targetId));
        if (request.title() != null) {
            target.setTitle(request.title());
        }
        if (request.jd() != null) {
            target.setJd(request.jd());
        }
        if (request.status() != null) {
            if (!VALID_STATUSES.contains(request.status())) {
                throw new IllegalArgumentException("Invalid status: " + request.status() + ". Must be one of: " + VALID_STATUSES);
            }
            target.setStatus(request.status());
        }
        target = targetRepository.save(target);
        return toDto(target);
    }

    @Transactional
    public void deleteTarget(UUID targetId, UUID userId) {
        InterviewTarget target = targetRepository.findByIdAndUserId(targetId, userId)
                .orElseThrow(() -> new TargetNotFoundException(targetId));
        targetRepository.delete(target);
    }

    private InterviewTargetDto toDto(InterviewTarget target) {
        return new InterviewTargetDto(
                target.getId().toString(),
                target.getUser().getId().toString(),
                target.getTitle(),
                target.getJd(),
                target.getStatus(),
                target.getCreatedAt().toString(),
                target.getUpdatedAt().toString()
        );
    }
}
