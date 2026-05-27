package com.interviewcoach.coachingmemory.controller;

import com.interviewcoach.coachingmemory.service.CoachingMemoryService;
import com.interviewcoach.common.api.CoachingMemoryDto;
import com.interviewcoach.common.security.SecurityUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/coaching-memories")
public class CoachingMemoryController {

    private final CoachingMemoryService memoryService;

    public CoachingMemoryController(CoachingMemoryService memoryService) {
        this.memoryService = memoryService;
    }

    @GetMapping("/target/{targetId}")
    public List<CoachingMemoryDto> getByTarget(@PathVariable UUID targetId) {
        return memoryService.getMemories(targetId, SecurityUtils.currentUser().getId());
    }

    @GetMapping("/{id}")
    public CoachingMemoryDto get(@PathVariable UUID id) {
        return memoryService.getMemory(id, SecurityUtils.currentUser().getId());
    }
}
