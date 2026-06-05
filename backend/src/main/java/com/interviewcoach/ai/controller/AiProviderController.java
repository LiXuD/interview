package com.interviewcoach.ai.controller;

import com.interviewcoach.ai.service.AiProviderService;
import com.interviewcoach.common.api.AiProviderCreateRequest;
import com.interviewcoach.common.api.AiProviderDto;
import com.interviewcoach.common.api.AiProviderModelsRequest;
import com.interviewcoach.common.api.AiProviderModelsResponse;
import com.interviewcoach.common.api.AiRuntimeStatusDto;
import com.interviewcoach.common.api.AiProviderTestRequest;
import com.interviewcoach.common.api.AiProviderTestResponse;
import com.interviewcoach.common.security.SecurityUtils;
import com.interviewcoach.user.entity.User;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * AI Provider 管理 REST 控制器。提供用户自定义 OpenAI-compatible Provider 的增删查改、
 * 连接测试、模型列表和运行时状态查询接口。
 */
@RestController
@RequestMapping("/api/ai-providers")
public class AiProviderController {

    private final AiProviderService providerService;

    public AiProviderController(AiProviderService providerService) {
        this.providerService = providerService;
    }

    /**
     * 获取当前用户的所有 AI Provider 列表
     *
     * @return Provider DTO 列表
     */
    @GetMapping
    public List<AiProviderDto> list() {
        User user = SecurityUtils.currentUser();
        return providerService.listProviders(user.getId());
    }

    /**
     * 获取当前用户的 AI 运行时状态（stub/平台真实AI/用户自定义Provider）
     *
     * @return 运行时状态 DTO
     */
    @GetMapping("/status")
    public AiRuntimeStatusDto status() {
        User user = SecurityUtils.currentUser();
        return providerService.getRuntimeStatus(user.getId());
    }

    /**
     * 创建新的 AI Provider
     *
     * @param request Provider 创建请求
     * @return 创建后的 Provider DTO
     */
    @PostMapping
    public AiProviderDto create(@RequestBody AiProviderCreateRequest request) {
        User user = SecurityUtils.currentUser();
        return providerService.createProvider(user, request);
    }

    /**
     * 测试 AI Provider 连接是否可用
     *
     * @param request Provider 测试请求
     * @return 测试结果
     */
    @PostMapping("/test")
    public AiProviderTestResponse test(@RequestBody AiProviderTestRequest request) {
        return providerService.testProvider(request);
    }

    /**
     * 获取指定 Provider 可用的模型列表
     *
     * @param request 模型列表请求
     * @return 模型列表响应
     */
    @PostMapping("/models")
    public AiProviderModelsResponse models(@RequestBody AiProviderModelsRequest request) {
        return providerService.listModels(request);
    }

    /**
     * 将指定 Provider 设为当前用户的默认 Provider
     *
     * @param id Provider ID
     * @return 更新后的 Provider DTO
     */
    @PatchMapping("/{id}/default")
    public AiProviderDto setDefault(@PathVariable UUID id) {
        User user = SecurityUtils.currentUser();
        return providerService.setDefault(id, user.getId());
    }

    /**
     * 删除指定 Provider 并清除其加密密钥
     *
     * @param id Provider ID
     * @return 204 No Content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        User user = SecurityUtils.currentUser();
        providerService.deleteProvider(id, user.getId());
        return ResponseEntity.noContent().build();
    }
}
