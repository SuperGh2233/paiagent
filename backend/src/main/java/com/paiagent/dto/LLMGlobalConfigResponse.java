package com.paiagent.dto;

import com.paiagent.entity.LLMGlobalConfig;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Public API representation of an LLM config. The secret itself is never serialized.
 */
public record LLMGlobalConfigResponse(
        Long id,
        String provider,
        String configName,
        String apiUrl,
        boolean apiKeyConfigured,
        String model,
        String ttsModel,
        String embeddingModel,
        String imageModel,
        String videoModel,
        Integer memoryEnabled,
        BigDecimal temperature,
        Integer isDefault,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static LLMGlobalConfigResponse from(LLMGlobalConfig config) {
        if (config == null) {
            return null;
        }
        return new LLMGlobalConfigResponse(
                config.getId(),
                config.getProvider(),
                config.getConfigName(),
                config.getApiUrl(),
                config.getApiKey() != null && !config.getApiKey().isBlank(),
                config.getModel(),
                config.getTtsModel(),
                config.getEmbeddingModel(),
                config.getImageModel(),
                config.getVideoModel(),
                config.getMemoryEnabled(),
                config.getTemperature(),
                config.getIsDefault(),
                config.getCreatedAt(),
                config.getUpdatedAt()
        );
    }
}
