package com.interviewcoach.target.controller;

import com.interviewcoach.common.api.InterviewTargetCreateRequest;
import com.interviewcoach.common.api.InterviewTargetDto;
import com.interviewcoach.common.api.InterviewTargetUpdateRequest;
import com.interviewcoach.target.service.InterviewTargetService;
import com.interviewcoach.user.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
            Authentication authentication,
            @RequestBody InterviewTargetCreateRequest request) {
        User user = currentUser(authentication);
        InterviewTargetDto dto = targetService.createTarget(user, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping
    public List<InterviewTargetDto> listTargets(Authentication authentication) {
        User user = currentUser(authentication);
        return targetService.listTargets(user.getId());
    }

    @GetMapping("/{id}")
    public InterviewTargetDto getTarget(
            Authentication authentication,
            @PathVariable("id") UUID id) {
        User user = currentUser(authentication);
        return targetService.getTarget(id, user.getId());
    }

    @PatchMapping("/{id}")
    public InterviewTargetDto updateTarget(
            Authentication authentication,
            @PathVariable("id") UUID id,
            @RequestBody InterviewTargetUpdateRequest request) {
        User user = currentUser(authentication);
        return targetService.updateTarget(id, user.getId(), request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTarget(
            Authentication authentication,
            @PathVariable("id") UUID id) {
        User user = currentUser(authentication);
        targetService.deleteTarget(id, user.getId());
        return ResponseEntity.noContent().build();
    }

    private User currentUser(Authentication authentication) {
        return (User) authentication.getPrincipal();
    }
}
