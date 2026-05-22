package com.interviewcoach.ai.controller;

import com.interviewcoach.ai.service.AiProviderService;
import com.interviewcoach.common.api.AiProviderCreateRequest;
import com.interviewcoach.common.api.AiProviderDto;
import com.interviewcoach.common.api.AiProviderTestRequest;
import com.interviewcoach.common.api.AiProviderTestResponse;
import com.interviewcoach.common.security.SecurityUtils;
import com.interviewcoach.user.entity.User;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/ai-providers")
public class AiProviderController {

    private final AiProviderService providerService;

    public AiProviderController(AiProviderService providerService) {
        this.providerService = providerService;
    }

    @GetMapping
    public List<AiProviderDto> list() {
        User user = SecurityUtils.currentUser();
        return providerService.listProviders(user.getId());
    }

    @PostMapping
    public AiProviderDto create(@RequestBody AiProviderCreateRequest request) {
        User user = SecurityUtils.currentUser();
        return providerService.createProvider(user, request);
    }

    @PostMapping("/test")
    public AiProviderTestResponse test(@RequestBody AiProviderTestRequest request) {
        return providerService.testProvider(request);
    }

    @PatchMapping("/{id}/default")
    public AiProviderDto setDefault(@PathVariable UUID id) {
        User user = SecurityUtils.currentUser();
        return providerService.setDefault(id, user.getId());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        User user = SecurityUtils.currentUser();
        providerService.deleteProvider(id, user.getId());
        return ResponseEntity.noContent().build();
    }
}
