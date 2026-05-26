package com.interviewcoach.target.controller;

import com.interviewcoach.common.api.InterviewTargetCreateRequest;
import com.interviewcoach.common.api.InterviewTargetDto;
import com.interviewcoach.common.api.InterviewTargetUpdateRequest;
import com.interviewcoach.common.security.SecurityUtils;
import com.interviewcoach.target.service.InterviewTargetService;
import com.interviewcoach.user.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/targets")
public class InterviewTargetController {

    private final InterviewTargetService targetService;

    public InterviewTargetController(InterviewTargetService targetService) {
        this.targetService = targetService;
    }

    @PostMapping
    public ResponseEntity<InterviewTargetDto> createTarget(
            @RequestBody InterviewTargetCreateRequest request) {
        User user = SecurityUtils.currentUser();
        InterviewTargetDto dto = targetService.createTarget(user, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping
    public List<InterviewTargetDto> listTargets() {
        User user = SecurityUtils.currentUser();
        return targetService.listTargets(user.getId());
    }

    @GetMapping("/{id}")
    public InterviewTargetDto getTarget(@PathVariable("id") UUID id) {
        User user = SecurityUtils.currentUser();
        return targetService.getTarget(id, user.getId());
    }

    @PatchMapping("/{id}")
    public InterviewTargetDto updateTarget(
            @PathVariable("id") UUID id,
            @RequestBody InterviewTargetUpdateRequest request) {
        User user = SecurityUtils.currentUser();
        return targetService.updateTarget(id, user.getId(), request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTarget(@PathVariable("id") UUID id) {
        User user = SecurityUtils.currentUser();
        targetService.deleteTarget(id, user.getId());
        return ResponseEntity.noContent().build();
    }
}
