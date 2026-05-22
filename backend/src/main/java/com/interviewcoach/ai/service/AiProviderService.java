package com.interviewcoach.ai.service;

import com.interviewcoach.ai.entity.AiProvider;
import com.interviewcoach.ai.repository.AiProviderRepository;
import com.interviewcoach.common.api.AiProviderCreateRequest;
import com.interviewcoach.common.api.AiProviderDto;
import com.interviewcoach.common.api.AiProviderTestRequest;
import com.interviewcoach.common.api.AiProviderTestResponse;
import com.interviewcoach.common.error.AiProviderNotFoundException;
import com.interviewcoach.user.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class AiProviderService {

    private static final Set<String> VALID_OPENAI_API_MODES = Set.of("chatCompletions", "responses");

    private final AiProviderRepository providerRepository;
    private final ApiKeyEncryption encryption;
    private final OpenAiCompatibleClient openAiClient;

    public AiProviderService(AiProviderRepository providerRepository,
                             ApiKeyEncryption encryption,
                             OpenAiCompatibleClient openAiClient) {
        this.providerRepository = providerRepository;
        this.encryption = encryption;
        this.openAiClient = openAiClient;
    }

    @Transactional
    public AiProviderDto createProvider(User user, AiProviderCreateRequest request) {
        validateOpenaiApiMode(request.openaiApiMode());

        AiProvider provider = new AiProvider();
        provider.setUser(user);
        provider.setName(request.name());
        provider.setBaseUrl(request.baseUrl());
        provider.setApiKeyEncrypted(encryption.encrypt(request.apiKey()));
        provider.setModel(request.model());
        provider.setOpenaiApiMode(request.openaiApiMode());

        boolean isFirst = !providerRepository.existsByUserId(user.getId());
        provider.setDefault(isFirst);

        provider = providerRepository.save(provider);
        return toDto(provider);
    }

    @Transactional(readOnly = true)
    public List<AiProviderDto> listProviders(UUID userId) {
        return providerRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toDto)
                .toList();
    }

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

    @Transactional
    public AiProviderDto setDefault(UUID providerId, UUID userId) {
        AiProvider provider = providerRepository.findByIdAndUserId(providerId, userId)
                .orElseThrow(() -> new AiProviderNotFoundException(providerId));

        providerRepository.findByUserIdAndIsDefaultTrue(userId)
                .ifPresent(current -> {
                    if (!current.getId().equals(providerId)) {
                        current.setDefault(false);
                        providerRepository.save(current);
                    }
                });

        provider.setDefault(true);
        provider = providerRepository.save(provider);
        return toDto(provider);
    }

    @Transactional
    public void deleteProvider(UUID providerId, UUID userId) {
        AiProvider provider = providerRepository.findByIdAndUserId(providerId, userId)
                .orElseThrow(() -> new AiProviderNotFoundException(providerId));
        providerRepository.delete(provider);
    }

    @Transactional(readOnly = true)
    public AiProvider findDefaultProvider(UUID userId) {
        return providerRepository.findByUserIdAndIsDefaultTrue(userId).orElse(null);
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
