package com.interviewcoach.ai.service;

import com.interviewcoach.ai.entity.AiProvider;
import com.interviewcoach.ai.repository.AiProviderRepository;
import com.interviewcoach.common.api.AiProviderCreateRequest;
import com.interviewcoach.common.api.AiProviderDto;
import com.interviewcoach.common.api.AiProviderModelsRequest;
import com.interviewcoach.common.api.AiProviderModelsResponse;
import com.interviewcoach.common.api.AiRuntimeStatusDto;
import com.interviewcoach.common.api.AiProviderTestRequest;
import com.interviewcoach.common.api.AiProviderTestResponse;
import com.interviewcoach.common.error.AiProviderNotFoundException;
import com.interviewcoach.user.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * AI Provider 业务服务。管理用户自定义 OpenAI-compatible Provider 的 CRUD、
 * 连接测试、模型列表、默认 Provider 设置和运行时状态查询。
 */
@Service
public class AiProviderService {

    private static final Set<String> VALID_OPENAI_API_MODES = Set.of("chatCompletions", "responses");

    private final AiProviderRepository providerRepository;
    private final ApiKeyEncryption encryption;
    private final OpenAiCompatibleClient openAiClient;
    private final PlatformAiProperties platformProperties;

    public AiProviderService(AiProviderRepository providerRepository,
                             ApiKeyEncryption encryption,
                             OpenAiCompatibleClient openAiClient,
                             PlatformAiProperties platformProperties) {
        this.providerRepository = providerRepository;
        this.encryption = encryption;
        this.openAiClient = openAiClient;
        this.platformProperties = platformProperties;
    }

    /**
     * 创建新的 AI Provider。首个 Provider 自动设为默认。
     * API Key 会通过 AES-GCM 加密后存储。
     *
     * @param user    当前用户
     * @param request Provider 创建请求
     * @return 创建后的 Provider DTO
     * @throws IllegalArgumentException openaiApiMode 不合法时
     */
    @Transactional
    public AiProviderDto createProvider(User user, AiProviderCreateRequest request) {
        // 1. 校验 API 模式是否合法
        validateOpenaiApiMode(request.openaiApiMode());

        // 2. 构建实体并加密 API Key
        AiProvider provider = new AiProvider();
        provider.setUser(user);
        provider.setName(request.name());
        provider.setBaseUrl(request.baseUrl());
        provider.setApiKeyEncrypted(encryption.encrypt(request.apiKey()));
        provider.setModel(request.model());
        provider.setOpenaiApiMode(request.openaiApiMode());

        // 3. 首个 Provider 自动设为默认
        boolean isFirst = !providerRepository.existsByUserId(user.getId());
        provider.setDefault(isFirst);

        // 4. 持久化并返回 DTO
        provider = providerRepository.save(provider);
        return toDto(provider);
    }

    /**
     * 获取指定用户的所有 Provider 列表，按创建时间倒序
     *
     * @param userId 用户 ID
     * @return Provider DTO 列表
     */
    @Transactional(readOnly = true)
    public List<AiProviderDto> listProviders(UUID userId) {
        return providerRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * 测试 Provider 连接是否可用
     *
     * @param request Provider 测试请求
     * @return 测试结果，包含成功/失败状态和消息
     */
    public AiProviderTestResponse testProvider(AiProviderTestRequest request) {
        validateOpenaiApiMode(request.openaiApiMode());
        try {
            openAiClient.testConnection(
                    request.baseUrl(),
                    request.apiKey(),
                    request.model(),
                    request.openaiApiMode()
            );
            return new AiProviderTestResponse(true, "连接成功");
        } catch (Exception ex) {
            return new AiProviderTestResponse(false, "连接失败: " + ex.getMessage());
        }
    }

    /**
     * 获取指定 Provider 可用的模型列表
     *
     * @param request 模型列表请求
     * @return 模型列表响应
     */
    public AiProviderModelsResponse listModels(AiProviderModelsRequest request) {
        return new AiProviderModelsResponse(openAiClient.listModels(request.baseUrl(), request.apiKey()));
    }

    /**
     * 将指定 Provider 设为默认，取消原默认 Provider
     *
     * @param providerId 目标 Provider ID
     * @param userId     当前用户 ID
     * @return 更新后的 Provider DTO
     * @throws AiProviderNotFoundException Provider 不存在或不属于当前用户时
     */
    @Transactional
    public AiProviderDto setDefault(UUID providerId, UUID userId) {
        // 1. 校验 Provider 归属
        AiProvider provider = providerRepository.findByIdAndUserId(providerId, userId)
                .orElseThrow(() -> new AiProviderNotFoundException(providerId));

        // 2. 取消原默认 Provider
        providerRepository.findByUserIdAndIsDefaultTrue(userId)
                .ifPresent(current -> {
                    if (!current.getId().equals(providerId)) {
                        current.setDefault(false);
                        providerRepository.save(current);
                    }
                });

        // 3. 设置新默认并持久化
        provider.setDefault(true);
        provider = providerRepository.save(provider);
        return toDto(provider);
    }

    /**
     * 删除指定 Provider，同时清除加密密钥
     *
     * @param providerId 目标 Provider ID
     * @param userId     当前用户 ID
     * @throws AiProviderNotFoundException Provider 不存在或不属于当前用户时
     */
    @Transactional
    public void deleteProvider(UUID providerId, UUID userId) {
        AiProvider provider = providerRepository.findByIdAndUserId(providerId, userId)
                .orElseThrow(() -> new AiProviderNotFoundException(providerId));
        providerRepository.delete(provider);
    }

    /**
     * 查找用户的默认 Provider，不存在返回 null
     *
     * @param userId 用户 ID
     * @return 默认 Provider 实体，或 null
     */
    @Transactional(readOnly = true)
    public AiProvider findDefaultProvider(UUID userId) {
        return providerRepository.findByUserIdAndIsDefaultTrue(userId).orElse(null);
    }

    /**
     * 获取当前 AI 运行时状态。
     * 优先级：用户自定义 Provider > 平台真实 AI > 本地 stub。
     *
     * @param userId 用户 ID
     * @return 运行时状态 DTO
     */
    @Transactional(readOnly = true)
    public AiRuntimeStatusDto getRuntimeStatus(UUID userId) {
        AiProvider provider = findDefaultProvider(userId);
        if (provider != null) {
            return new AiRuntimeStatusDto(
                    "realUserProvider",
                    true,
                    "userOpenAICompatible",
                    "Using user default OpenAI-compatible Provider.");
        }
        if (platformProperties.isEnabled()) {
            if (platformProperties.isComplete()) {
                return new AiRuntimeStatusDto(
                        "realPlatformProvider",
                        true,
                        "platformDefault",
                        "Using platform OpenAI-compatible Provider.");
            }
            return new AiRuntimeStatusDto(
                    "unavailable",
                    false,
                    "platformDefault",
                    "Platform AI is enabled but configuration is incomplete.");
        }
        return new AiRuntimeStatusDto(
                "stubOnly",
                false,
                "platformDefault",
                "Only LocalPlatformAiClient stub is available.");
    }

    private AiProviderDto toDto(AiProvider provider) {
        return new AiProviderDto(
                provider.getId().toString(),
                provider.getName(),
                provider.getBaseUrl(),
                provider.getModel(),
                provider.getOpenaiApiMode(),
                provider.isDefault(),
                provider.getCreatedAt().toString()
        );
    }

    private void validateOpenaiApiMode(String mode) {
        if (!VALID_OPENAI_API_MODES.contains(mode)) {
            throw new IllegalArgumentException("Invalid openaiApiMode: " + mode + ". Must be one of: chatCompletions, responses");
        }
    }
}
